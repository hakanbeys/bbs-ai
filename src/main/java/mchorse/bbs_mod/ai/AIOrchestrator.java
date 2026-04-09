package mchorse.bbs_mod.ai;

import mchorse.bbs_mod.ai.client.*;
import mchorse.bbs_mod.ai.log.AIOperationLogger;
import mchorse.bbs_mod.ai.session.AISession;
import mchorse.bbs_mod.ai.session.AISessionManager;
import mchorse.bbs_mod.ai.tools.*;
import mchorse.bbs_mod.ai.validation.AIValidator;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.utils.undo.UndoManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Core AI engine. Manages the conversation loop:
 * 1. Receives user message
 * 2. Enhances prompt with scene context
 * 3. Sends to AI provider
 * 4. Executes tool calls in a loop
 * 5. Returns final response
 *
 * Supports approval flow, auto-save, cancellation, and validation.
 */
public class AIOrchestrator
{
    private static final int MAX_TOOL_ITERATIONS = 20;

    private final AISessionManager sessionManager = new AISessionManager();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /* Listeners for UI updates */
    private Consumer<OrchestratorEvent> eventListener;

    /* Pending approval state */
    private PendingApproval pendingApproval;

    public AISessionManager getSessionManager()
    {
        return this.sessionManager;
    }

    public void setEventListener(Consumer<OrchestratorEvent> listener)
    {
        this.eventListener = listener;
    }

    public boolean isRunning()
    {
        return this.running.get();
    }

    public void cancel()
    {
        this.cancelled.set(true);
    }

    public PendingApproval getPendingApproval()
    {
        return this.pendingApproval;
    }

    /**
     * Send a user message and process the AI response loop.
     */
    public CompletableFuture<Void> sendMessage(String userText, Film film, String filmId, UndoManager undoManager)
    {
        return sendMessage(userText, film, filmId, undoManager, null);
    }

    public CompletableFuture<Void> sendMessage(String userText, Film film, String filmId, UndoManager undoManager, byte[] imageData)
    {
        if (this.running.get())
        {
            fireEvent(OrchestratorEvent.error("AI is already processing a request."));
            return CompletableFuture.completedFuture(null);
        }

        this.running.set(true);
        this.cancelled.set(false);
        this.pendingApproval = null;

        return CompletableFuture.runAsync(() ->
        {
            try
            {
                processMessage(userText, film, filmId, undoManager, imageData);
            }
            catch (Exception e)
            {
                fireEvent(OrchestratorEvent.error("Unexpected error: " + e.getMessage()));
                AIOperationLogger.logError(filmId, "Orchestrator error: " + e.getMessage());
            }
            finally
            {
                this.running.set(false);
                fireEvent(OrchestratorEvent.finished());
            }
        });
    }

    private void processMessage(String userText, Film film, String filmId, UndoManager undoManager, byte[] imageData)
    {
        AISession session = sessionManager.getOrCreateSession(filmId);
        FilmMemory memory = new FilmMemory(filmId);

        /* Enhance the user message with scene context */
        String enhanced = PromptEnhancer.enhance(userText, film);

        /* Build user message */
        AIMessage userMsg = AIMessage.user(enhanced);
        if (imageData != null)
        {
            userMsg.withImage(imageData, "image/png");
        }
        session.addMessage(userMsg);

        /* Log the user message */
        AIOperationLogger.logMessage(filmId, "USER", userText);
        fireEvent(OrchestratorEvent.userMessage(userText));

        /* Build the system prompt */
        String systemPromptText = AISettings.systemPrompt.get() + memory.toContextString();

        /* Create tool context */
        AIToolContext toolContext = new AIToolContext(film, filmId, undoManager);

        /* Run the conversation loop */
        int iterations = 0;
        while (iterations < MAX_TOOL_ITERATIONS && !cancelled.get())
        {
            iterations++;

            /* Build request */
            AIRequest request = buildRequest(session, systemPromptText);
            fireEvent(OrchestratorEvent.thinking());

            /* Send to AI */
            AIResponse response;
            try
            {
                response = AIHttpClient.sendRequest(request).join();
            }
            catch (Exception e)
            {
                fireEvent(OrchestratorEvent.error("Failed to contact AI: " + e.getMessage()));
                return;
            }

            if (cancelled.get()) return;

            if (response.isError())
            {
                fireEvent(OrchestratorEvent.error(response.getError()));
                AIOperationLogger.logError(filmId, response.getError());
                return;
            }

            /* Add assistant response to session */
            if (response.getContent() != null && !response.getContent().isEmpty())
            {
                session.addMessage(AIMessage.assistant(response.getContent()));
                fireEvent(OrchestratorEvent.assistantMessage(response.getContent()));
                AIOperationLogger.logMessage(filmId, "ASSISTANT", response.getContent());
            }

            /* If no tool calls, we're done */
            if (!response.hasToolCalls())
            {
                break;
            }

            /* Process tool calls */
            boolean requireApproval = AISettings.requireApproval.get();
            List<ToolExecution> executions = new ArrayList<>();
            int totalChanges = 0;

            for (AIToolCall toolCall : response.getToolCalls())
            {
                if (cancelled.get()) return;

                IAITool tool = AIToolRegistry.getTool(toolCall.getFunctionName());
                if (tool == null)
                {
                    String errorMsg = "Unknown tool: " + toolCall.getFunctionName();
                    session.addMessage(AIMessage.toolResult(toolCall.getId(), toolCall.getFunctionName(), errorMsg));
                    fireEvent(OrchestratorEvent.toolResult(toolCall.getFunctionName(), errorMsg, false));
                    continue;
                }

                /* Execute tool */
                fireEvent(OrchestratorEvent.toolCall(toolCall.getFunctionName(), toolCall.getArgumentsJson()));

                AIToolResult result = tool.execute(toolCall.getArgumentsJson(), toolContext);
                executions.add(new ToolExecution(toolCall, result));
                totalChanges += result.getChangesCount();

                /* Log the tool call */
                AIOperationLogger.log(filmId, toolCall.getFunctionName(), toolCall.getArgumentsJson(), result.getContent());

                /* Add tool result to session */
                session.addMessage(AIMessage.toolResult(toolCall.getId(), toolCall.getFunctionName(), result.getContent()));
                fireEvent(OrchestratorEvent.toolResult(toolCall.getFunctionName(), result.getContent(), result.isSuccess()));
            }

            /* Check if approval is needed for write operations */
            if (requireApproval && totalChanges > 0)
            {
                this.pendingApproval = new PendingApproval(executions, totalChanges);
                fireEvent(OrchestratorEvent.approvalRequired(totalChanges));
                /* The UI will call approveChanges() or rejectChanges() */
                return;
            }

            /* Run validation after tool execution */
            if (totalChanges > 0)
            {
                AIValidator.validate(film, filmId);
            }

            /* Handle preview request */
            if (toolContext.isPreviewRequested())
            {
                fireEvent(OrchestratorEvent.previewRequested(
                    toolContext.getPreviewFromTick(),
                    toolContext.getPreviewToTick()
                ));
            }
        }

        if (iterations >= MAX_TOOL_ITERATIONS)
        {
            fireEvent(OrchestratorEvent.error("AI exceeded maximum tool iterations (" + MAX_TOOL_ITERATIONS + ")."));
        }
    }

    /**
     * Approve pending changes. In approval mode, changes are already applied
     * through BaseValue.edit() — approval just confirms them.
     */
    public void approveChanges()
    {
        if (this.pendingApproval != null)
        {
            fireEvent(OrchestratorEvent.approved(this.pendingApproval.totalChanges));
            this.pendingApproval = null;
        }
    }

    /**
     * Reject pending changes — undo them via the undo system.
     */
    public void rejectChanges(Film film, UndoManager undoManager)
    {
        if (this.pendingApproval != null)
        {
            int undoCount = this.pendingApproval.totalChanges;
            /* Undo each write operation */
            for (int i = 0; i < this.pendingApproval.executions.size(); i++)
            {
                ToolExecution exec = this.pendingApproval.executions.get(i);
                if (exec.result.getChangesCount() > 0)
                {
                    undoManager.undo(film);
                }
            }
            fireEvent(OrchestratorEvent.rejected(undoCount));
            this.pendingApproval = null;
        }
    }

    private AIRequest buildRequest(AISession session, String systemPrompt)
    {
        AIRequest request = new AIRequest(AISettings.aiModel.get());
        request.setMaxTokens(4096);
        request.setTemperature(0.7f);

        /* Add system prompt */
        request.addMessage(AIMessage.system(systemPrompt));

        /* Add session messages */
        for (AIMessage msg : session.getMessages())
        {
            request.addMessage(msg);
        }

        /* Add tool definitions */
        for (AIRequest.AIToolDefinition def : AIToolRegistry.getToolDefinitions())
        {
            request.addTool(def);
        }

        return request;
    }

    private void fireEvent(OrchestratorEvent event)
    {
        if (this.eventListener != null)
        {
            this.eventListener.accept(event);
        }
    }

    /* Inner classes */

    public static class ToolExecution
    {
        public final AIToolCall toolCall;
        public final AIToolResult result;

        public ToolExecution(AIToolCall toolCall, AIToolResult result)
        {
            this.toolCall = toolCall;
            this.result = result;
        }
    }

    public static class PendingApproval
    {
        public final List<ToolExecution> executions;
        public final int totalChanges;

        public PendingApproval(List<ToolExecution> executions, int totalChanges)
        {
            this.executions = executions;
            this.totalChanges = totalChanges;
        }
    }

    /**
     * Events emitted by the orchestrator for UI consumption.
     */
    public static class OrchestratorEvent
    {
        public enum Type
        {
            USER_MESSAGE, ASSISTANT_MESSAGE, THINKING,
            TOOL_CALL, TOOL_RESULT,
            APPROVAL_REQUIRED, APPROVED, REJECTED,
            PREVIEW_REQUESTED,
            ERROR, FINISHED
        }

        public final Type type;
        public final String content;
        public final String toolName;
        public final boolean success;
        public final int changeCount;
        public final int fromTick;
        public final int toTick;

        private OrchestratorEvent(Type type, String content, String toolName, boolean success, int changeCount, int fromTick, int toTick)
        {
            this.type = type;
            this.content = content;
            this.toolName = toolName;
            this.success = success;
            this.changeCount = changeCount;
            this.fromTick = fromTick;
            this.toTick = toTick;
        }

        public static OrchestratorEvent userMessage(String content) { return new OrchestratorEvent(Type.USER_MESSAGE, content, null, true, 0, 0, 0); }
        public static OrchestratorEvent assistantMessage(String content) { return new OrchestratorEvent(Type.ASSISTANT_MESSAGE, content, null, true, 0, 0, 0); }
        public static OrchestratorEvent thinking() { return new OrchestratorEvent(Type.THINKING, null, null, true, 0, 0, 0); }
        public static OrchestratorEvent toolCall(String toolName, String args) { return new OrchestratorEvent(Type.TOOL_CALL, args, toolName, true, 0, 0, 0); }
        public static OrchestratorEvent toolResult(String toolName, String result, boolean success) { return new OrchestratorEvent(Type.TOOL_RESULT, result, toolName, success, 0, 0, 0); }
        public static OrchestratorEvent approvalRequired(int count) { return new OrchestratorEvent(Type.APPROVAL_REQUIRED, null, null, true, count, 0, 0); }
        public static OrchestratorEvent approved(int count) { return new OrchestratorEvent(Type.APPROVED, null, null, true, count, 0, 0); }
        public static OrchestratorEvent rejected(int count) { return new OrchestratorEvent(Type.REJECTED, null, null, false, count, 0, 0); }
        public static OrchestratorEvent previewRequested(int from, int to) { return new OrchestratorEvent(Type.PREVIEW_REQUESTED, null, null, true, 0, from, to); }
        public static OrchestratorEvent error(String message) { return new OrchestratorEvent(Type.ERROR, message, null, false, 0, 0, 0); }
        public static OrchestratorEvent finished() { return new OrchestratorEvent(Type.FINISHED, null, null, true, 0, 0, 0); }
    }
}
