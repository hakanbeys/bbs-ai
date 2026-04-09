package mchorse.bbs_mod.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.ai.client.*;
import mchorse.bbs_mod.ai.log.AIDiagnosticTrace;
import mchorse.bbs_mod.ai.log.AIOperationLogger;
import mchorse.bbs_mod.ai.session.AISession;
import mchorse.bbs_mod.ai.session.AISessionManager;
import mchorse.bbs_mod.ai.tools.*;
import mchorse.bbs_mod.ai.validation.AIValidator;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.utils.undo.UndoManager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final int MAX_RECOVERY_ATTEMPTS = 2;
    private static final Pattern ACTOR_SELECT_PATTERN = Pattern.compile("Best actor match for \\\".*?\\\": \\[(\\d+)] \\\"([^\\\"]+)\\\"");

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
        AIDiagnosticTrace trace = new AIDiagnosticTrace(filmId, UUID.randomUUID().toString());

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
        String systemPromptText = AISettings.systemPrompt().get() + memory.toContextString();
        trace.start(AISettings.aiProvider().get(), AISettings.aiModel().get(), userText, enhanced, systemPromptText);

        /* Create tool context */
        AIToolContext toolContext = new AIToolContext(film, filmId, undoManager);
        updateSessionStage(session, determineStartingStage(userText), "Starting from the first required workflow phase.");

        /* Run the conversation loop */
        int iterations = 0;
        int recoveryAttempts = 0;
        Map<AISession.WorkflowStage, Integer> stageStalls = new EnumMap<>(AISession.WorkflowStage.class);
        while (iterations < MAX_TOOL_ITERATIONS && !cancelled.get())
        {
            iterations++;
            AIAgentPrompts.RequestProfile profile = AIAgentPrompts.analyze(userText, session);
            reconcileStageFromProfile(session, profile);
            AISession.WorkflowStage stageBeforeRequest = session.getWorkflowStage();
            trace.iteration(iterations, session.getWorkflowStage(), session.getWorkflowHint());

            /* Build request */
            AIRequest request = buildRequest(session, systemPromptText, iterations, recoveryAttempts, userText, profile);
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

            trace.response(response.getFinishReason(), response.getPromptTokens(), response.getCompletionTokens(), response.hasToolCalls(), response.getContent());

            /* Add assistant response to session */
            if (response.hasToolCalls())
            {
                session.addMessage(AIMessage.assistantToolCalls(response.getContent(), response.getToolCalls()));
            }
            else if (response.getContent() != null && !response.getContent().isEmpty())
            {
                session.addMessage(AIMessage.assistant(response.getContent()));
            }

            if (response.getContent() != null && !response.getContent().isEmpty())
            {
                fireEvent(OrchestratorEvent.assistantMessage(response.getContent()));
                AIOperationLogger.logMessage(filmId, "ASSISTANT", response.getContent());
            }

            /* If no tool calls, we're done */
            if (!response.hasToolCalls())
            {
                if (shouldRecoverWithTools(userText, response, recoveryAttempts, profile))
                {
                    recoveryAttempts++;
                    incrementStageStall(stageStalls, stageBeforeRequest);
                    updateSessionStage(session, AISession.WorkflowStage.RECOVERY, "Assistant answer did not advance the film. Switching back to explicit tool usage.");
                    session.addMessage(AIMessage.system(AIAgentPrompts.buildRecoveryPrompt(recoveryAttempts)));
                    trace.recovery("Assistant answered without advancing the film.");
                    continue;
                }

                if (shouldForceStageContinuation(session, profile, recoveryAttempts, response))
                {
                    recoveryAttempts++;
                    int stallCount = incrementStageStall(stageStalls, stageBeforeRequest);
                    updateSessionStage(session, AISession.WorkflowStage.RECOVERY, "The request is still mid-workflow. Forcing the next required stage.");
                    session.addMessage(AIMessage.system(AIAgentPrompts.buildRecoveryPrompt(recoveryAttempts)));

                    String enforcement = AIAgentPrompts.buildStageEnforcementPrompt(profile);

                    if (!enforcement.isEmpty())
                    {
                        session.addMessage(AIMessage.system(enforcement));
                    }

                    if (stallCount >= 2)
                    {
                        String fallback = AIAgentPrompts.buildStageFallbackPrompt(profile);

                        if (!fallback.isEmpty())
                        {
                            session.addMessage(AIMessage.system(fallback));
                        }
                    }

                    trace.recovery("Workflow continuation forced because the agent stopped mid-stage.");
                    continue;
                }

                if (response.getContent() != null && !response.getContent().isEmpty())
                {
                    updateStageFromAssistantContent(session, response.getContent(), profile);
                }

                if (profile.actionable && session.getWorkflowStage() == AISession.WorkflowStage.PREVIEW)
                {
                    updateSessionStage(session, AISession.WorkflowStage.COMPLETE, "Preview-ready result reached. Awaiting user review or next iteration.");
                }

                resetStageStall(stageStalls, stageBeforeRequest);

                break;
            }

            /* Process tool calls */
            boolean requireApproval = AISettings.requireApproval().get();
            List<ToolExecution> executions = new ArrayList<>();
            int totalChanges = 0;
            List<AIAgentPrompts.ToolFailure> failedTools = new ArrayList<>();

            for (AIToolCall toolCall : response.getToolCalls())
            {
                if (cancelled.get()) return;

                AIToolCall effectiveToolCall = hydrateToolCallFromMemory(session, toolCall);
                IAITool tool = AIToolRegistry.getTool(effectiveToolCall.getFunctionName());
                if (tool == null)
                {
                    String errorMsg = "Unknown tool: " + effectiveToolCall.getFunctionName();
                    session.addMessage(AIMessage.toolResult(effectiveToolCall.getId(), effectiveToolCall.getFunctionName(), errorMsg));
                    fireEvent(OrchestratorEvent.toolResult(effectiveToolCall.getFunctionName(), errorMsg, false));
                    continue;
                }

                /* Execute tool */
                fireEvent(OrchestratorEvent.toolCall(effectiveToolCall.getFunctionName(), effectiveToolCall.getArgumentsJson()));
                trace.toolCall(effectiveToolCall.getFunctionName(), effectiveToolCall.getArgumentsJson());

                AIToolResult result = tool.execute(effectiveToolCall.getArgumentsJson(), toolContext);
                executions.add(new ToolExecution(effectiveToolCall, result));
                totalChanges += result.getChangesCount();
                trace.toolResult(effectiveToolCall.getFunctionName(), result.isSuccess(), result.getChangesCount(), result.getContent());

                if (!result.isSuccess())
                {
                    failedTools.add(new AIAgentPrompts.ToolFailure(effectiveToolCall.getFunctionName(), result.getContent()));
                }
                else
                {
                    updateSessionMemoryFromTool(session, effectiveToolCall.getFunctionName(), effectiveToolCall.getArgumentsJson(), result.getContent());
                    updateStageFromToolSuccess(session, effectiveToolCall.getFunctionName(), profile);
                    resetStageStall(stageStalls, stageBeforeRequest);
                }

                /* Log the tool call */
                AIOperationLogger.log(filmId, effectiveToolCall.getFunctionName(), effectiveToolCall.getArgumentsJson(), result.getContent());

                /* Add tool result to session */
                session.addMessage(AIMessage.toolResult(effectiveToolCall.getId(), effectiveToolCall.getFunctionName(), result.getContent()));
                fireEvent(OrchestratorEvent.toolResult(effectiveToolCall.getFunctionName(), result.getContent(), result.isSuccess()));
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
                recoveryAttempts = 0;
            }
            else if (!failedTools.isEmpty() && recoveryAttempts < MAX_RECOVERY_ATTEMPTS)
            {
                recoveryAttempts++;
                int stallCount = incrementStageStall(stageStalls, stageBeforeRequest);
                updateSessionStage(session, AISession.WorkflowStage.RECOVERY, "One or more tool calls failed. Re-inspecting before the next action.");
                session.addMessage(AIMessage.system(AIAgentPrompts.buildToolFailurePrompt(failedTools)));
                trace.recovery("Tool failure triggered recovery.");

                if (stallCount >= 2)
                {
                    String fallback = AIAgentPrompts.buildStageFallbackPrompt(profile);

                    if (!fallback.isEmpty())
                    {
                        session.addMessage(AIMessage.system(fallback));
                    }
                }
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
            trace.finish("max_iterations");
        }
        else if (cancelled.get())
        {
            trace.finish("cancelled");
        }
        else
        {
            trace.finish("completed");
        }

        session.setDiagnostic(trace.buildSummary(), trace.getTracePath());
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

    private AIRequest buildRequest(AISession session, String systemPrompt, int iteration, int recoveryAttempts, String userText, AIAgentPrompts.RequestProfile profile)
    {
        AIRequest request = new AIRequest(AISettings.aiModel().get());
        request.setMaxTokens(8192);
        request.setTemperature(iteration <= 2 ? 0.45f : 0.35f);

        /* Add system prompt */
        request.addMessage(AIMessage.system(systemPrompt));
        request.addMessage(AIMessage.system(AIAgentPrompts.buildToolGuide()));
        request.addMessage(AIMessage.system(AIAgentPrompts.buildWorkflowPrompt(profile, iteration)));

        if (recoveryAttempts > 0)
        {
            request.addMessage(AIMessage.system(AIAgentPrompts.buildRecoveryPrompt(recoveryAttempts)));
        }

        String stageEnforcement = AIAgentPrompts.buildStageEnforcementPrompt(profile);

        if (!stageEnforcement.isEmpty())
        {
            request.addMessage(AIMessage.system(stageEnforcement));
        }

        if (profile.actionable)
        {
            request.addMessage(AIMessage.system("The latest user request is actionable inside BBS. Prefer using tools over pure explanation."));
        }

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

    private boolean shouldRecoverWithTools(String userText, AIResponse response, int recoveryAttempts, AIAgentPrompts.RequestProfile profile)
    {
        if (recoveryAttempts >= MAX_RECOVERY_ATTEMPTS)
        {
            return false;
        }

        if (!looksLikeActionRequest(userText))
        {
            return false;
        }

        String content = response.getContent();

        if (content == null || content.isEmpty())
        {
            return true;
        }

        String lowered = content.toLowerCase();
        boolean hasQuestionCard = lowered.contains("<question-card");
        boolean hasTodoCard = lowered.contains("<todo-card");

        if (hasQuestionCard || hasTodoCard)
        {
            return false;
        }

        if (profile.broadCreativeRequest && !profile.hasRecentQuestionCard)
        {
            return true;
        }

        return lowered.contains("i can") || lowered.contains("i'll") || lowered.contains("would you like") || lowered.contains("use ")
            || lowered.contains("could") || lowered.contains("should") || lowered.contains("yapabilirim")
            || lowered.contains("istersen");
    }

    private boolean looksLikeActionRequest(String text)
    {
        if (text == null)
        {
            return false;
        }

        String lowered = text.toLowerCase();

        return lowered.contains("create") || lowered.contains("make") || lowered.contains("animate") || lowered.contains("scene")
            || lowered.contains("camera") || lowered.contains("actor") || lowered.contains("model") || lowered.contains("move")
            || lowered.contains("walk") || lowered.contains("run") || lowered.contains("jump") || lowered.contains("fight")
            || lowered.contains("cinematic") || lowered.contains("shot") || lowered.contains("form") || lowered.contains("pose")
            || lowered.contains("yap") || lowered.contains("oluştur") || lowered.contains("animasyon") || lowered.contains("sahne")
            || lowered.contains("kamera") || lowered.contains("aktör") || lowered.contains("model") || lowered.contains("hareket");
    }

    private void fireEvent(OrchestratorEvent event)
    {
        if (this.eventListener != null)
        {
            this.eventListener.accept(event);
        }
    }

    private void updateStageFromAssistantContent(AISession session, String content, AIAgentPrompts.RequestProfile profile)
    {
        String lowered = content.toLowerCase();

        if (lowered.contains("<question-card"))
        {
            updateSessionStage(session, AISession.WorkflowStage.DIRECTION, "Waiting for the user to pick a creative direction.");
            return;
        }

        if (lowered.contains("<todo-card"))
        {
            updateSessionStage(session, AISession.WorkflowStage.DISCOVERY, "Execution plan was presented. Next step is to follow it with tools.");
            return;
        }

        if (!profile.actionable)
        {
            updateSessionStage(session, AISession.WorkflowStage.COMPLETE, "Non-actionable guidance response completed.");
        }
    }

    private void updateStageFromToolSuccess(AISession session, String toolName, AIAgentPrompts.RequestProfile profile)
    {
        if ("scene_info".equals(toolName) || "actor_list".equals(toolName) || "asset_search".equals(toolName) || "model_search".equals(toolName) || "form_inspect".equals(toolName) || "camera_inspect".equals(toolName))
        {
            updateSessionStage(session, AISession.WorkflowStage.DISCOVERY, "Discovery is in progress; exact targets are being inspected.");
            return;
        }

        if ("actor_add".equals(toolName) || "actor_form".equals(toolName) || "form_property_edit".equals(toolName) || "form_property_keyframes".equals(toolName) || "form_tool".equals(toolName))
        {
            updateSessionStage(session, AISession.WorkflowStage.BLOCKING, "Actor setup or form blocking is underway.");
            return;
        }

        if ("actor_path".equals(toolName) || "replay_tool".equals(toolName) || "action_tool".equals(toolName) || "sync_tool".equals(toolName) || "generate_scene".equals(toolName))
        {
            if (profile.cameraFocused)
            {
                updateSessionStage(session, AISession.WorkflowStage.CAMERA, "Motion exists; the next focus should be camera staging.");
            }
            else
            {
                updateSessionStage(session, AISession.WorkflowStage.BLOCKING, "Actor motion is being authored.");
            }

            return;
        }

        if ("camera_shot".equals(toolName) || "camera_tool".equals(toolName) || "camera_clear".equals(toolName))
        {
            updateSessionStage(session, AISession.WorkflowStage.CAMERA, "Camera timeline is being built or refined.");
            return;
        }

        if ("preview_tool".equals(toolName))
        {
            updateSessionStage(session, AISession.WorkflowStage.PREVIEW, "Preview has been requested for review.");
        }
    }

    private void updateSessionStage(AISession session, AISession.WorkflowStage stage, String hint)
    {
        session.setWorkflowStage(stage, hint);
    }

    private int incrementStageStall(Map<AISession.WorkflowStage, Integer> stageStalls, AISession.WorkflowStage stage)
    {
        if (stage == null)
        {
            return 0;
        }

        int value = stageStalls.getOrDefault(stage, 0) + 1;
        stageStalls.put(stage, value);

        return value;
    }

    private void resetStageStall(Map<AISession.WorkflowStage, Integer> stageStalls, AISession.WorkflowStage stage)
    {
        if (stage != null)
        {
            stageStalls.remove(stage);
        }
    }

    private AISession.WorkflowStage determineStartingStage(String userText)
    {
        String lowered = userText == null ? "" : userText.toLowerCase();

        if (lowered.contains("cinematic") || lowered.contains("scene") || lowered.contains("storyboard")
            || lowered.contains("sinematik") || lowered.contains("sahne"))
        {
            return AISession.WorkflowStage.DIRECTION;
        }

        return AISession.WorkflowStage.DISCOVERY;
    }

    private void reconcileStageFromProfile(AISession session, AIAgentPrompts.RequestProfile profile)
    {
        if (!profile.actionable)
        {
            return;
        }

        if (profile.hasRecentPreview)
        {
            updateSessionStage(session, AISession.WorkflowStage.PREVIEW, "Preview has already been requested or completed.");
            return;
        }

        if (profile.hasRecentCameraWrite)
        {
            updateSessionStage(session, AISession.WorkflowStage.CAMERA, "Camera coverage is active; focus on review or final refinements.");
            return;
        }

        if (profile.hasRecentActorMotionWrite || profile.hasRecentFormWrite)
        {
            if (profile.cameraFocused || profile.broadCreativeRequest)
            {
                updateSessionStage(session, AISession.WorkflowStage.CAMERA, "Actor motion exists; next priority is camera staging.");
            }
            else
            {
                updateSessionStage(session, AISession.WorkflowStage.BLOCKING, "Animation blocking is underway.");
            }

            return;
        }

        if (profile.hasRecentQuestionCard && profile.broadCreativeRequest && !profile.hasRecentActorList)
        {
            updateSessionStage(session, AISession.WorkflowStage.DIRECTION, "Waiting for direction before full discovery.");
            return;
        }

        updateSessionStage(session, AISession.WorkflowStage.DISCOVERY, "Continue inspecting exact targets before writing.");
    }

    private boolean shouldForceStageContinuation(AISession session, AIAgentPrompts.RequestProfile profile, int recoveryAttempts, AIResponse response)
    {
        if (recoveryAttempts >= MAX_RECOVERY_ATTEMPTS || !profile.actionable)
        {
            return false;
        }

        String content = response.getContent() == null ? "" : response.getContent().toLowerCase();

        if (content.contains("<question-card") || content.contains("<todo-card"))
        {
            return false;
        }

        AISession.WorkflowStage stage = session.getWorkflowStage();

        if (stage == null)
        {
            return false;
        }

        return stage == AISession.WorkflowStage.DISCOVERY
            || stage == AISession.WorkflowStage.BLOCKING
            || stage == AISession.WorkflowStage.CAMERA
            || stage == AISession.WorkflowStage.RECOVERY;
    }

    private void updateSessionMemoryFromTool(AISession session, String toolName, String argumentsJson, String resultContent)
    {
        JsonObject args = parseArgs(argumentsJson);

        if (hasInt(args, "actor_index"))
        {
            session.rememberActor(args.get("actor_index").getAsInt(), null, null);
        }

        if (hasString(args, "model_key"))
        {
            session.rememberModelKey(args.get("model_key").getAsString());
        }

        if (hasString(args, "property_path"))
        {
            session.rememberPropertyPath(args.get("property_path").getAsString());
        }

        if ("actor_select".equals(toolName))
        {
            Matcher matcher = ACTOR_SELECT_PATTERN.matcher(resultContent == null ? "" : resultContent);

            if (matcher.find())
            {
                session.rememberActor(Integer.parseInt(matcher.group(1)), matcher.group(2), getString(args, "query"));
            }
        }
        else if ("actor_form".equals(toolName) || ("model_search".equals(toolName) && "assign".equals(getString(args, "action"))))
        {
            session.rememberActor(getInteger(args, "actor_index"), null, null);
            session.rememberModelKey(getString(args, "model_key"));
        }
        else if ("form_inspect".equals(toolName))
        {
            session.rememberActor(getInteger(args, "actor_index"), null, null);
        }
        else if ("camera_shot".equals(toolName) || "camera_tool".equals(toolName) || "camera_clear".equals(toolName) || "camera_preset".equals(toolName) || "camera_inspect".equals(toolName))
        {
            String cameraAction = getString(args, "preset");

            if (cameraAction.isEmpty())
            {
                cameraAction = getString(args, "action");
            }

            if (cameraAction.isEmpty())
            {
                cameraAction = toolName;
            }

            session.rememberCameraAction(cameraAction);
        }
    }

    private JsonObject parseArgs(String argumentsJson)
    {
        try
        {
            return JsonParser.parseString(AIJsonRepair.repairObject(argumentsJson)).getAsJsonObject();
        }
        catch (Exception e)
        {
            return new JsonObject();
        }
    }

    private boolean hasInt(JsonObject args, String key)
    {
        return args.has(key) && args.get(key).isJsonPrimitive() && args.get(key).getAsJsonPrimitive().isNumber();
    }

    private boolean hasString(JsonObject args, String key)
    {
        return args.has(key) && args.get(key).isJsonPrimitive() && args.get(key).getAsJsonPrimitive().isString();
    }

    private String getString(JsonObject args, String key)
    {
        return hasString(args, key) ? args.get(key).getAsString() : "";
    }

    private Integer getInteger(JsonObject args, String key)
    {
        return hasInt(args, key) ? args.get(key).getAsInt() : null;
    }

    private AIToolCall hydrateToolCallFromMemory(AISession session, AIToolCall toolCall)
    {
        JsonObject args = parseArgs(toolCall.getArgumentsJson());
        boolean changed = false;

        if (session.getSelectedActorIndex() != null && usesActorIndex(toolCall.getFunctionName()) && !hasInt(args, "actor_index"))
        {
            args.addProperty("actor_index", session.getSelectedActorIndex());
            changed = true;
        }

        if (session.getSelectedActorIndex() != null && usesFocusActorIndex(toolCall.getFunctionName()) && !hasInt(args, "focus_actor_index"))
        {
            args.addProperty("focus_actor_index", session.getSelectedActorIndex());
            changed = true;
        }

        if (usesModelKey(toolCall.getFunctionName()) && session.describeWorkingMemory() != null && !session.describeWorkingMemory().isEmpty() && !hasString(args, "model_key"))
        {
            String modelKey = session.describeWorkingMemory();
            if (session.describeWorkingMemory().contains("active model_key: "))
            {
                modelKey = extractMemoryValue(session.describeWorkingMemory(), "active model_key: ");
                if (!modelKey.isEmpty())
                {
                    args.addProperty("model_key", modelKey);
                    changed = true;
                }
            }
        }

        if (usesPropertyPath(toolCall.getFunctionName()) && !hasString(args, "property_path"))
        {
            String propertyPath = extractMemoryValue(session.describeWorkingMemory(), "active property_path: ");
            if (!propertyPath.isEmpty())
            {
                args.addProperty("property_path", propertyPath);
                changed = true;
            }
        }

        if (!changed)
        {
            return toolCall;
        }

        return new AIToolCall(toolCall.getId(), toolCall.getFunctionName(), args.toString());
    }

    private boolean usesActorIndex(String toolName)
    {
        return "actor_form".equals(toolName)
            || "action_tool".equals(toolName)
            || "replay_tool".equals(toolName)
            || "actor_path".equals(toolName)
            || "motion_preset".equals(toolName)
            || "form_inspect".equals(toolName)
            || "form_property_edit".equals(toolName)
            || "form_property_keyframes".equals(toolName)
            || "form_tool".equals(toolName)
            || "model_search".equals(toolName);
    }

    private boolean usesFocusActorIndex(String toolName)
    {
        return "camera_preset".equals(toolName);
    }

    private boolean usesModelKey(String toolName)
    {
        return "actor_form".equals(toolName) || "model_search".equals(toolName);
    }

    private boolean usesPropertyPath(String toolName)
    {
        return "form_property_edit".equals(toolName)
            || "form_property_keyframes".equals(toolName)
            || "form_tool".equals(toolName);
    }

    private String extractMemoryValue(String summary, String prefix)
    {
        if (summary == null || summary.isEmpty())
        {
            return "";
        }

        String[] lines = summary.split("\\r?\\n");

        for (String line : lines)
        {
            String trimmed = line.trim();

            if (trimmed.startsWith("- " + prefix))
            {
                return trimmed.substring(("- " + prefix).length()).trim();
            }
        }

        return "";
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
