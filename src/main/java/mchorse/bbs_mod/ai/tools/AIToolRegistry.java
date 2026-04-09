package mchorse.bbs_mod.ai.tools;

import mchorse.bbs_mod.ai.client.AIRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of all AI tools available for the AI animator.
 */
public class AIToolRegistry
{
    private static final Map<String, IAITool> tools = new LinkedHashMap<>();

    static
    {
        register(new SceneInfoTool());
        register(new ActorListTool());
        register(new AssetSearchTool());
        register(new ModelSearchTool());
        register(new ActorAddTool());
        register(new ActorFormTool());
        register(new FormInspectTool());
        register(new FormPropertyTool());
        register(new FormPropertyKeyframesTool());
        register(new ActionTool());
        register(new ActorPathTool());
        register(new ReplayTool());
        register(new CameraInspectTool());
        register(new CameraClearTool());
        register(new CameraShotTool());
        register(new SyncTool());
        register(new PreviewTool());
        register(new WebSearchTool());
    }

    public static void register(IAITool tool)
    {
        tools.put(tool.getName(), tool);
    }

    public static IAITool getTool(String name)
    {
        return tools.get(name);
    }

    public static Map<String, IAITool> getAllTools()
    {
        return tools;
    }

    /**
     * Convert all registered tools to AIRequest.AIToolDefinition list
     * for inclusion in API requests.
     */
    public static List<AIRequest.AIToolDefinition> getToolDefinitions()
    {
        List<AIRequest.AIToolDefinition> definitions = new ArrayList<>();

        for (IAITool tool : tools.values())
        {
            definitions.add(new AIRequest.AIToolDefinition(
                tool.getName(),
                tool.getDescription(),
                tool.getParametersSchema()
            ));
        }

        return definitions;
    }
}
