package mchorse.bbs_mod.ai;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;

/**
 * Delegates to BBSSettings for all AI configuration.
 * Provides convenient accessors used by the AI subsystem.
 */
public class AISettings
{
    public static ValueString aiProvider() { return BBSSettings.aiProvider; }
    public static ValueString aiModel() { return BBSSettings.aiModel; }
    public static ValueString aiApiKey() { return BBSSettings.aiApiKey; }
    public static ValueString aiBaseUrl() { return BBSSettings.aiBaseUrl; }
    public static ValueString webSearchProvider() { return BBSSettings.aiWebSearchProvider; }
    public static ValueString webSearchApiKey() { return BBSSettings.aiWebSearchApiKey; }
    public static ValueString systemPrompt() { return BBSSettings.aiSystemPrompt; }
    public static ValueInt maxKeyframesPerOperation() { return BBSSettings.aiMaxKeyframes; }
    public static ValueBoolean autoSaveBeforeAI() { return BBSSettings.aiAutoSave; }
    public static ValueBoolean requireApproval() { return BBSSettings.aiRequireApproval; }

    public static final String DEFAULT_SYSTEM_PROMPT = BBSSettings.DEFAULT_AI_SYSTEM_PROMPT;
}
