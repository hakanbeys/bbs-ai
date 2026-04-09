package mchorse.bbs_mod.ai.tools;

public class AIToolResult
{
    private final boolean success;
    private final String content;
    private final int changesCount;

    public AIToolResult(boolean success, String content)
    {
        this(success, content, 0);
    }

    public AIToolResult(boolean success, String content, int changesCount)
    {
        this.success = success;
        this.content = content;
        this.changesCount = changesCount;
    }

    public static AIToolResult success(String content)
    {
        return new AIToolResult(true, content);
    }

    public static AIToolResult success(String content, int changes)
    {
        return new AIToolResult(true, content, changes);
    }

    public static AIToolResult error(String message)
    {
        return new AIToolResult(false, "ERROR: " + message);
    }

    public boolean isSuccess()
    {
        return this.success;
    }

    public String getContent()
    {
        return this.content;
    }

    public int getChangesCount()
    {
        return this.changesCount;
    }
}
