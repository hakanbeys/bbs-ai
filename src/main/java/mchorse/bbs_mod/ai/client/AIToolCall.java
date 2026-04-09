package mchorse.bbs_mod.ai.client;

public class AIToolCall
{
    private final String id;
    private final String functionName;
    private final String argumentsJson;

    public AIToolCall(String id, String functionName, String argumentsJson)
    {
        this.id = id;
        this.functionName = functionName;
        this.argumentsJson = argumentsJson;
    }

    public String getId()
    {
        return this.id;
    }

    public String getFunctionName()
    {
        return this.functionName;
    }

    public String getArgumentsJson()
    {
        return this.argumentsJson;
    }
}
