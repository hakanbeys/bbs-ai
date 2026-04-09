package mchorse.bbs_mod.ai.tools;

public interface IAITool
{
    String getName();

    String getDescription();

    String getParametersSchema();

    AIToolResult execute(String argumentsJson, AIToolContext context);
}
