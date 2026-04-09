package mchorse.bbs_mod.ai.client;

import java.util.ArrayList;
import java.util.List;

public class AIResponse
{
    private String content;
    private List<AIToolCall> toolCalls = new ArrayList<>();
    private String finishReason;
    private int promptTokens;
    private int completionTokens;
    private String error;

    public AIResponse()
    {}

    public String getContent()
    {
        return this.content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public List<AIToolCall> getToolCalls()
    {
        return this.toolCalls;
    }

    public void addToolCall(AIToolCall toolCall)
    {
        this.toolCalls.add(toolCall);
    }

    public boolean hasToolCalls()
    {
        return !this.toolCalls.isEmpty();
    }

    public String getFinishReason()
    {
        return this.finishReason;
    }

    public void setFinishReason(String finishReason)
    {
        this.finishReason = finishReason;
    }

    public int getPromptTokens()
    {
        return this.promptTokens;
    }

    public void setPromptTokens(int promptTokens)
    {
        this.promptTokens = promptTokens;
    }

    public int getCompletionTokens()
    {
        return this.completionTokens;
    }

    public void setCompletionTokens(int completionTokens)
    {
        this.completionTokens = completionTokens;
    }

    public String getError()
    {
        return this.error;
    }

    public void setError(String error)
    {
        this.error = error;
    }

    public boolean isError()
    {
        return this.error != null && !this.error.isEmpty();
    }
}
