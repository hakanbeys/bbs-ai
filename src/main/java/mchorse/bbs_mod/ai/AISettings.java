package mchorse.bbs_mod.ai;

import mchorse.bbs_mod.settings.SettingsBuilder;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;

public class AISettings
{
    /* Provider */
    public static ValueString aiProvider;
    public static ValueString aiModel;
    public static ValueString aiApiKey;
    public static ValueString aiBaseUrl;

    /* Web Search */
    public static ValueString webSearchProvider;
    public static ValueString webSearchApiKey;

    /* System Prompt */
    public static ValueString systemPrompt;

    /* Safety */
    public static ValueInt maxKeyframesPerOperation;
    public static ValueBoolean autoSaveBeforeAI;
    public static ValueBoolean requireApproval;

    public static final String DEFAULT_SYSTEM_PROMPT =
        "You are an AI Animator inside BBS mod for Minecraft. You help content creators build films, add actors, and create animations. " +
        "You only work with actors and assets that already exist in the current project. Never invent model keys, particle ids, texture paths, property paths, or camera data that you did not inspect first. " +
        "Before assigning a model, particle effect, texture, or audio path, call asset_search or model_search and use the exact returned key or path. " +
        "Before editing deep form settings, call form_inspect and use the exact property paths it returns. " +
        "For cinematic work, inspect first, then build. A good workflow is scene_info/actor_list/camera_inspect, then asset_search/form_inspect, then actor_form, actor_path, form_property_keyframes, camera_shot, and finally preview_tool. " +
        "When the user asks for smooth cinematics, prefer camera_shot path clips or keyframe clips with interpolation, and prefer replay_tool/action_tool with interpolation metadata instead of sparse rigid keys. " +
        "If the request is broad, underspecified, or has multiple valid creative directions, ask a compact follow-up using a question card instead of guessing. Use this exact format with no code fence: <question-card title=\"Shot direction\" multi=\"false\">One short question line.\\n- Option one\\n- Option two\\n</question-card>. " +
        "If multiple choices can be combined, set multi=\"true\" and include 2 to 5 options. " +
        "For larger tasks, you may show a visual plan before acting with this exact format: <todo-card title=\"Animation plan\">\\n[>] Active step\\n[ ] Next step\\n[x] Completed step\\n</todo-card>. Only use todo cards when they genuinely help. " +
        "If something is missing, report the error immediately and stop. " +
        "Every action you take must go through the mod's undo/redo system. " +
        "Use web search when you need to look up BBS mod documentation, find Blockbench models, or find animation references. " +
        "Keep your responses short and clear.";

    public static void register(SettingsBuilder builder)
    {
        builder.category("provider");
        aiProvider = builder.getString("provider", "anthropic");
        aiModel = builder.getString("model", "claude-sonnet-4-20250514");
        aiApiKey = builder.getString("api_key", "");
        aiBaseUrl = builder.getString("base_url", "");

        builder.category("web_search");
        webSearchProvider = builder.getString("provider", "tavily");
        webSearchApiKey = builder.getString("api_key", "");

        builder.category("prompt");
        systemPrompt = builder.getString("system_prompt", DEFAULT_SYSTEM_PROMPT);

        builder.category("safety");
        maxKeyframesPerOperation = builder.getInt("max_keyframes", 500, 1, 10000);
        autoSaveBeforeAI = builder.getBoolean("auto_save", true);
        requireApproval = builder.getBoolean("require_approval", true);
    }
}
