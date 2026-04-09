package mchorse.bbs_mod.ai.log;

import mchorse.bbs_mod.ai.session.AISession;

import java.io.File;
import java.time.Duration;

public class AIDiagnosticTrace
{
    private final String filmId;
    private final String runId;
    private final long startedAt = System.currentTimeMillis();
    private File traceFile;
    private int iterations;
    private int toolCalls;
    private int toolFailures;
    private int recoveries;
    private String lastStage = "IDLE";
    private String finishReason = "unfinished";

    public AIDiagnosticTrace(String filmId, String runId)
    {
        this.filmId = filmId;
        this.runId = runId;
    }

    public void start(String provider, String model, String userText, String enhancedText, String systemPrompt)
    {
        this.traceFile = AIOperationLogger.createTraceFile(this.filmId, this.runId);
        this.log("RUN", "provider=" + provider + ", model=" + model);
        this.log("USER_RAW", userText);
        this.log("USER_ENHANCED", enhancedText);
        this.log("SYSTEM_PROMPT", AIOperationLogger.safeTruncate(systemPrompt, 3000));
    }

    public void iteration(int iteration, AISession.WorkflowStage stage, String hint)
    {
        this.iterations = Math.max(this.iterations, iteration);
        this.lastStage = stage == null ? "IDLE" : stage.name();
        this.log("ITERATION", "#" + iteration + " stage=" + this.lastStage + " hint=" + (hint == null ? "" : hint));
    }

    public void response(String finishReason, int promptTokens, int completionTokens, boolean hasToolCalls, String content)
    {
        this.finishReason = finishReason == null || finishReason.isEmpty() ? this.finishReason : finishReason;
        this.log(
            "RESPONSE",
            "finish_reason=" + this.finishReason +
                ", prompt_tokens=" + promptTokens +
                ", completion_tokens=" + completionTokens +
                ", has_tool_calls=" + hasToolCalls +
                ", content=" + AIOperationLogger.safeTruncate(content, 1200)
        );
    }

    public void toolCall(String toolName, String argumentsJson)
    {
        this.toolCalls += 1;
        this.log("TOOL_CALL", toolName + " args=" + AIOperationLogger.safeTruncate(argumentsJson, 1200));
    }

    public void toolResult(String toolName, boolean success, int changesCount, String content)
    {
        if (!success)
        {
            this.toolFailures += 1;
        }

        this.log(
            "TOOL_RESULT",
            toolName + " success=" + success + ", changes=" + changesCount + ", result=" + AIOperationLogger.safeTruncate(content, 1200)
        );
    }

    public void recovery(String reason)
    {
        this.recoveries += 1;
        this.log("RECOVERY", reason);
    }

    public void finish(String finishReason)
    {
        if (finishReason != null && !finishReason.isEmpty())
        {
            this.finishReason = finishReason;
        }

        long elapsed = Duration.ofMillis(System.currentTimeMillis() - this.startedAt).toMillis();
        this.log(
            "SUMMARY",
            "iterations=" + this.iterations +
                ", tool_calls=" + this.toolCalls +
                ", tool_failures=" + this.toolFailures +
                ", recoveries=" + this.recoveries +
                ", last_stage=" + this.lastStage +
                ", finish_reason=" + this.finishReason +
                ", elapsed_ms=" + elapsed
        );
    }

    public String buildSummary()
    {
        return "Diag: " +
            this.iterations + " iter, " +
            this.toolCalls + " tools, " +
            this.toolFailures + " fails, " +
            this.recoveries + " recoveries, " +
            "stage=" + this.lastStage +
            ", finish=" + this.finishReason;
    }

    public String getTracePath()
    {
        return this.traceFile == null ? "" : this.traceFile.getAbsolutePath();
    }

    private void log(String kind, String content)
    {
        AIOperationLogger.appendTrace(this.traceFile, kind, content);
    }
}
