package mchorse.bbs_mod.ai;

import mchorse.bbs_mod.ai.client.AIMessage;
import mchorse.bbs_mod.ai.session.AISession;
import mchorse.bbs_mod.ai.tools.IAITool;
import mchorse.bbs_mod.ai.tools.AIToolRegistry;

import java.util.List;
import java.util.Map;

public class AIAgentPrompts
{
    public static String buildToolGuide()
    {
        StringBuilder builder = new StringBuilder();

        builder.append("AGENT TOOL GUIDE\n");
        builder.append("You are operating tools, not imagining outcomes.\n");
        builder.append("Before any write action, inspect the scene and inspect the exact target.\n");
        builder.append("Required default order for edits: scene_info -> actor_list -> exact discovery tool -> write tool -> preview_tool when helpful.\n");
        builder.append("If the user asks for a new cinematic scene, prefer planning and then generate_scene or actor_add/actor_form/actor_path/camera_shot in sequence.\n");
        builder.append("If the request is broad and there are multiple reasonable creative directions, ask a question-card first.\n");
        builder.append("If a tool fails because an actor, model, property, or camera target is missing, do not guess; inspect again and recover.\n");
        builder.append("Available tools and intended use:\n");

        for (Map.Entry<String, IAITool> entry : AIToolRegistry.getAllTools().entrySet())
        {
            builder.append("- ").append(entry.getKey()).append(": ").append(entry.getValue().getDescription()).append("\n");
        }

        builder.append("Critical workflows:\n");
        builder.append("- Actor selection and movement: actor_list or actor_select -> actor_add if needed -> action_tool or actor_path or replay_tool.\n");
        builder.append("- Motion presets: motion_preset for reliable smooth walk, run, idle, jump, or dramatic entrance blocking.\n");
        builder.append("- Model assignment: model_search or asset_search -> actor_form.\n");
        builder.append("- Form editing: actor_list -> form_inspect -> form_property_edit or form_property_keyframes.\n");
        builder.append("- Camera work: camera_inspect -> camera_clear if replacing -> camera_shot.\n");
        builder.append("- Camera presets: camera_preset for reliable establishing, close-up, orbit, low-angle, and tracking shots.\n");
        builder.append("- Previewing: use preview_tool after substantial animation or camera edits.\n");

        return builder.toString();
    }

    public static String buildRecoveryPrompt(int iteration)
    {
        return "RECOVERY MODE " + iteration + ": If the last answer did not advance the film, switch to explicit tool usage. " +
            "Do not describe what you would do. Inspect, then act with tools. " +
            "If information is missing, ask a focused question-card.";
    }

    public static String buildStageEnforcementPrompt(RequestProfile profile)
    {
        if (!profile.actionable)
        {
            return "";
        }

        if (profile.broadCreativeRequest && !profile.hasRecentQuestionCard)
        {
            return "ENFORCEMENT: This is still a broad cinematic request. If multiple creative directions are plausible, your next assistant response must be a concise question-card instead of prose.";
        }

        if (!profile.hasRecentSceneInfo)
        {
            return "ENFORCEMENT: Before more edits, call scene_info now.";
        }

        if ((profile.actorFocused || profile.formFocused || profile.assetFocused || profile.broadCreativeRequest) && !profile.hasRecentActorList)
        {
            return "ENFORCEMENT: Before selecting or editing actors, call actor_list now.";
        }

        if (profile.assetFocused && !profile.hasRecentAssetDiscovery)
        {
            return "ENFORCEMENT: Before assigning or editing model-dependent content, call model_search or asset_search now.";
        }

        if (profile.formFocused && !profile.hasRecentFormInspect)
        {
            return "ENFORCEMENT: Before editing form properties, call form_inspect now.";
        }

        if ((profile.cameraFocused || profile.broadCreativeRequest) && !profile.hasRecentCameraInspect)
        {
            return "ENFORCEMENT: Before replacing or extending camera work, call camera_inspect now.";
        }

        if (profile.broadCreativeRequest && !profile.hasRecentActorMotionWrite)
        {
            return "ENFORCEMENT: Discovery is sufficient. Next, create actual actor motion with actor_path, replay_tool, action_tool, or generate_scene.";
        }

        if ((profile.broadCreativeRequest || profile.cameraFocused) && profile.hasRecentActorMotionWrite && !profile.hasRecentCameraWrite)
        {
            return "ENFORCEMENT: Actor motion exists. Next, build the camera timeline with camera_shot or camera_tool.";
        }

        if (profile.complexRequest && (profile.hasRecentActorMotionWrite || profile.hasRecentFormWrite) && profile.hasRecentCameraWrite && !profile.hasRecentPreview)
        {
            return "ENFORCEMENT: Motion and camera are ready enough. Next, call preview_tool or give a concise completion summary.";
        }

        return "";
    }

    public static String buildWorkflowPrompt(RequestProfile profile, int iteration)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("AGENT WORKFLOW\n");
        builder.append("You are inside a tool-rich animation editor. Advance the film instead of narrating intentions.\n");

        if (profile.actionable)
        {
            builder.append("This request is actionable. Use tools unless the user is explicitly asking for advice only.\n");
        }

        if (profile.broadCreativeRequest)
        {
            builder.append("This is a broad creative animation request. Start by reducing ambiguity.\n");

            if (!profile.hasRecentQuestionCard && iteration == 1)
            {
                builder.append("If there are multiple valid cinematic directions, your next assistant message should be a compact question-card before editing.\n");
            }

            if (!profile.hasRecentTodoCard)
            {
                builder.append("After direction is clear, you may emit one compact todo-card to show the execution plan, then continue with tools.\n");
            }
        }

        if (profile.complexRequest)
        {
            builder.append("The request is multi-step. Break it into discovery, blocking, camera, polish, and preview.\n");
        }

        if (profile.actorFocused)
        {
            builder.append("Actor workflow is mandatory: actor_list or actor_select before selecting, actor_add only when missing, then motion tools.\n");
        }

        if (profile.formFocused)
        {
            builder.append("Form workflow is mandatory: inspect exact actor first, then form_inspect, then property edits or property keyframes.\n");
        }

        if (profile.cameraFocused)
        {
            builder.append("Camera workflow is mandatory: inspect existing camera first, clear only if replacing, then create multi-shot camera work.\n");
        }

        if (profile.assetFocused)
        {
            builder.append("Asset workflow is mandatory: model_search or asset_search before actor_form or texture/property writes. Never invent asset keys.\n");
        }

        if (profile.workflowStage != null)
        {
            builder.append("Current workflow stage: ").append(profile.workflowStage.name()).append(".\n");
        }

        if (profile.workflowHint != null && !profile.workflowHint.isEmpty())
        {
            builder.append("Stage hint: ").append(profile.workflowHint).append("\n");
        }

        if (profile.workingMemorySummary != null && !profile.workingMemorySummary.isEmpty())
        {
            builder.append("Working memory:\n").append(profile.workingMemorySummary);
        }

        String stagePolicy = buildStagePolicyPrompt(profile);

        if (!stagePolicy.isEmpty())
        {
            builder.append(stagePolicy);
        }

        String prerequisitePrompt = buildPrerequisitePrompt(profile);

        if (!prerequisitePrompt.isEmpty())
        {
            builder.append(prerequisitePrompt);
        }

        builder.append("For polished cinematic output, do not stop at setup. Keep going until actors move, camera is staged, and preview is suggested when helpful.\n");

        return builder.toString();
    }

    public static String buildStagePolicyPrompt(RequestProfile profile)
    {
        if (profile.workflowStage == null)
        {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        switch (profile.workflowStage)
        {
            case DIRECTION:
                builder.append("Stage policy: reduce ambiguity first. Prefer question-card. Do not jump into heavy edits unless the direction is already clear.\n");
                break;
            case DISCOVERY:
                builder.append("Stage policy: inspect and discover exact targets. Prefer scene_info, actor_list, model_search, asset_search, form_inspect, camera_inspect.\n");
                builder.append("Stage policy: do not end with generic advice while discovery is still incomplete.\n");
                break;
            case BLOCKING:
                builder.append("Stage policy: create tangible actor setup and motion now. Prefer actor_add, actor_form, actor_path, replay_tool, action_tool, form_property_keyframes.\n");
                builder.append("Stage policy: do not stay in inspection unless a write just failed.\n");
                break;
            case CAMERA:
                builder.append("Stage policy: build or refine cinematic camera coverage now. Prefer camera_shot, camera_tool, camera_clear, then preview_tool.\n");
                builder.append("Stage policy: do not stop before there is actual camera work.\n");
                break;
            case PREVIEW:
                builder.append("Stage policy: result is preview-ready. Prefer preview_tool or a concise completion summary with next-step options.\n");
                break;
            case RECOVERY:
                builder.append("Stage policy: a previous step failed or stalled. Re-inspect the exact target, then perform one corrected tool action.\n");
                break;
            case COMPLETE:
                builder.append("Stage policy: the current request is in a completed state. Only continue editing if the user asks for another change.\n");
                break;
            default:
                break;
        }

        return builder.toString();
    }

    public static String buildStageFallbackPrompt(RequestProfile profile)
    {
        if (profile.workflowStage == null)
        {
            return "";
        }

        switch (profile.workflowStage)
        {
            case DIRECTION:
                return "STAGE FALLBACK: Stop freeform discussion. Ask one concise question-card with 2-4 cinematic direction options, or if direction is already obvious move to scene_info and actor_list.";
            case DISCOVERY:
                return "STAGE FALLBACK: Discovery stalled. Use only inspection tools next: scene_info, actor_list, model_search, asset_search, form_inspect, camera_inspect.";
            case BLOCKING:
                return "STAGE FALLBACK: Blocking stalled. Stop inspecting unless the last write failed. Create actual actor motion now with actor_path, replay_tool, action_tool, actor_form, or generate_scene.";
            case CAMERA:
                return "STAGE FALLBACK: Camera stage stalled. Build real coverage now with camera_shot or camera_tool, then use preview_tool.";
            case PREVIEW:
                return "STAGE FALLBACK: The scene is preview-ready. Use preview_tool or provide a concise completion summary.";
            case RECOVERY:
                return "STAGE FALLBACK: Recovery is stalled. Re-inspect the exact failing target, avoid repeating the same bad write, and perform one corrected tool call.";
            default:
                return "";
        }
    }

    public static String buildToolFailurePrompt(List<ToolFailure> failures)
    {
        StringBuilder builder = new StringBuilder();
        StringBuilder joined = new StringBuilder();

        for (int i = 0; i < failures.size(); i++)
        {
            ToolFailure failure = failures.get(i);

            if (i > 0)
            {
                joined.append(", ");
            }

            joined.append(failure.toolName);
        }

        builder.append("TOOL FAILURE RECOVERY: The last tool step did not advance the scene. Failed tools: ")
            .append(joined)
            .append(". Do not repeat the same failing write blindly.\n");

        for (ToolFailure failure : failures)
        {
            String hint = buildFailureHint(failure);

            builder.append("- ").append(failure.toolName).append(": ").append(hint).append("\n");
        }

        builder.append("Recover by re-inspecting the target, correcting the tool sequence, then acting again.");

        return builder.toString();
    }

    public static RequestProfile analyze(String userText, AISession session)
    {
        String lowered = normalize(userText);
        RequestProfile profile = new RequestProfile();

        profile.actionable = containsAny(lowered,
            "create", "make", "animate", "build", "stage", "edit", "update", "change", "move", "walk", "run", "jump",
            "fight", "pose", "block", "camera", "shot", "scene", "actor", "model", "form", "property", "keyframe",
            "yap", "olustur", "animasyon", "duzenle", "hareket", "kamera", "sahne", "aktor", "oyuncu", "poz");

        profile.cameraFocused = containsAny(lowered, "camera", "shot", "cinematic", "angle", "orbit", "close-up", "wide", "kamera", "sinematik");
        profile.actorFocused = containsAny(lowered, "actor", "character", "npc", "performer", "aktor", "oyuncu");
        profile.formFocused = containsAny(lowered, "form", "pose", "bone", "property", "model setting", "sekil", "poz", "property");
        profile.assetFocused = containsAny(lowered, "model", "texture", "particle", "asset", "skin", "kostum");

        profile.broadCreativeRequest = containsAny(lowered,
            "cinematic", "scene", "sequence", "shot list", "storyboard", "fight scene", "chase", "dramatic", "entrance",
            "film", "movie", "sinematik", "sahne", "sekans", "kovalamaca", "dovus", "hikaye", "storyboard")
            || wordCount(lowered) > 30;

        profile.complexRequest = profile.broadCreativeRequest
            || containsAny(lowered, "then", "after", "before", "meanwhile", "multiple", "several", "phases", "shots", "kamera degissin", "sonra", "ardindan", "birden fazla")
            || wordCount(lowered) > 18;

        profile.hasRecentQuestionCard = sessionContainsTag(session, "question-card", 8);
        profile.hasRecentTodoCard = sessionContainsTag(session, "todo-card", 8);
        profile.hasRecentSceneInfo = sessionContainsTool(session, "scene_info", 10);
        profile.hasRecentActorList = sessionContainsTool(session, "actor_list", 10);
        profile.hasRecentAssetDiscovery = sessionContainsAnyTool(session, 12, "asset_search", "model_search");
        profile.hasRecentFormInspect = sessionContainsTool(session, "form_inspect", 12);
        profile.hasRecentCameraInspect = sessionContainsTool(session, "camera_inspect", 12);
        profile.hasRecentPreview = sessionContainsTool(session, "preview_tool", 8);
        profile.hasRecentActorMotionWrite = sessionContainsAnyTool(session, 14, "actor_path", "replay_tool", "action_tool", "generate_scene", "sync_tool");
        profile.hasRecentFormWrite = sessionContainsAnyTool(session, 14, "actor_form", "form_property_edit", "form_property_keyframes", "form_tool");
        profile.hasRecentCameraWrite = sessionContainsAnyTool(session, 14, "camera_shot", "camera_tool", "generate_scene");
        profile.workflowStage = session.getWorkflowStage();
        profile.workflowHint = session.getWorkflowHint();
        profile.workingMemorySummary = session.describeWorkingMemory();

        return profile;
    }

    public static String buildPrerequisitePrompt(RequestProfile profile)
    {
        StringBuilder builder = new StringBuilder();

        if (profile.actionable && !profile.hasRecentSceneInfo)
        {
            builder.append("Missing prerequisite: call scene_info before making assumptions about duration or existing camera work.\n");
        }

        if ((profile.actorFocused || profile.formFocused || profile.assetFocused || profile.broadCreativeRequest) && !profile.hasRecentActorList)
        {
            builder.append("Missing prerequisite: call actor_list before selecting or editing actors.\n");
        }

        if (profile.assetFocused && !profile.hasRecentAssetDiscovery)
        {
            builder.append("Missing prerequisite: call model_search or asset_search before actor_form or asset-dependent edits.\n");
        }

        if (profile.formFocused && !profile.hasRecentFormInspect)
        {
            builder.append("Missing prerequisite: call form_inspect before form_property_edit or form_property_keyframes.\n");
        }

        if ((profile.cameraFocused || profile.broadCreativeRequest) && !profile.hasRecentCameraInspect)
        {
            builder.append("Missing prerequisite: call camera_inspect before replacing or extending the camera timeline.\n");
        }

        if (profile.complexRequest && profile.hasRecentCameraInspect && profile.hasRecentActorList && !profile.hasRecentPreview)
        {
            builder.append("After meaningful animation or camera edits, use preview_tool to make the result reviewable.\n");
        }

        if (profile.broadCreativeRequest)
        {
            if ((profile.hasRecentActorList || profile.hasRecentAssetDiscovery) && !profile.hasRecentActorMotionWrite)
            {
                builder.append("Progress checkpoint: discovery has started. Next, commit to actor blocking or generate_scene instead of doing more vague discussion.\n");
            }

            if (profile.hasRecentActorMotionWrite && !profile.hasRecentCameraWrite)
            {
                builder.append("Progress checkpoint: actor motion exists. Next, stage the camera with camera_shot or camera_tool. Do not stop before camera work.\n");
            }

            if ((profile.hasRecentActorMotionWrite || profile.hasRecentFormWrite) && profile.hasRecentCameraWrite && !profile.hasRecentPreview)
            {
                builder.append("Progress checkpoint: motion and camera are present. Next, use preview_tool or give a concise completion summary.\n");
            }
        }

        return builder.toString();
    }

    private static boolean sessionContainsTag(AISession session, String tag, int lookback)
    {
        List<AIMessage> messages = session.getMessages();
        int start = Math.max(0, messages.size() - lookback);

        for (int i = messages.size() - 1; i >= start; i--)
        {
            String content = messages.get(i).getContent();

            if (content != null && content.contains("<" + tag))
            {
                return true;
            }
        }

        return false;
    }

    private static boolean sessionContainsTool(AISession session, String toolName, int lookback)
    {
        return sessionContainsAnyTool(session, lookback, toolName);
    }

    private static boolean sessionContainsAnyTool(AISession session, int lookback, String... toolNames)
    {
        List<AIMessage> messages = session.getMessages();
        int start = Math.max(0, messages.size() - lookback);

        for (int i = messages.size() - 1; i >= start; i--)
        {
            AIMessage message = messages.get(i);

            if (message.getRole() != AIMessage.Role.TOOL)
            {
                continue;
            }

            String name = message.getToolName();

            if (name == null)
            {
                continue;
            }

            for (String toolName : toolNames)
            {
                if (toolName.equals(name))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private static String normalize(String text)
    {
        if (text == null)
        {
            return "";
        }

        return text.toLowerCase()
            .replace('ş', 's')
            .replace('ı', 'i')
            .replace('ğ', 'g')
            .replace('ü', 'u')
            .replace('ö', 'o')
            .replace('ç', 'c');
    }

    private static boolean containsAny(String text, String... needles)
    {
        for (String needle : needles)
        {
            if (text.contains(needle))
            {
                return true;
            }
        }

        return false;
    }

    private static int wordCount(String text)
    {
        String trimmed = text.trim();

        if (trimmed.isEmpty())
        {
            return 0;
        }

        return trimmed.split("\\s+").length;
    }

    private static String buildFailureHint(ToolFailure failure)
    {
        String message = normalize(failure.message);
        String toolName = failure.toolName == null ? "" : failure.toolName;

        if (message.contains("out of range") || message.contains("valid indices"))
        {
            return "actor selection is invalid. Call actor_list or actor_select, pick a valid actor_index, then retry.";
        }

        if (message.contains("no actors in the scene"))
        {
            return "the scene has no actors yet. Use actor_add or generate_scene before editing actor motion.";
        }

        if (message.contains("has no form assigned") || message.contains("has no form/model assigned"))
        {
            return "the target actor has no usable form. Use actor_select if needed, then actor_form with an exact model from model_search or asset_search.";
        }

        if (message.contains("property not found") || message.contains("not animatable"))
        {
            return "the property path is wrong or unsupported. Call form_inspect and use an exact animatable property path.";
        }

        if (message.contains("model_key is required"))
        {
            return "an exact model key is missing. Use model_search or asset_search first, then retry with model_key.";
        }

        if (message.contains("value is required"))
        {
            return "the write request is incomplete. Provide the required value and retry after inspection if needed.";
        }

        if (message.contains("points is required") || message.contains("at least one keyframe is required"))
        {
            return "camera data is incomplete. Inspect camera state, then send concrete camera points or keyframes.";
        }

        if (message.contains("too many keyframes"))
        {
            return "the request is too dense for one call. Split the animation into smaller tool calls or use generate_scene for full sequences.";
        }

        if (message.contains("unknown action"))
        {
            return "the requested tool action is unsupported. Re-check the tool schema and use a valid action.";
        }

        if (message.contains("api key") || message.contains("provider"))
        {
            return "this failure is configuration-related. Avoid relying on that tool unless settings are configured.";
        }

        if ("generate_scene".equals(toolName))
        {
            return "the scene payload was incomplete. If you cannot produce a full valid scene JSON, fall back to smaller actor and camera tools.";
        }

        if ("camera_shot".equals(toolName) || "camera_tool".equals(toolName))
        {
            return "camera setup needs clearer structured shot data. Re-inspect camera and rebuild the shot with explicit ticks and framing.";
        }

        if ("replay_tool".equals(toolName) || "action_tool".equals(toolName) || "actor_path".equals(toolName))
        {
            return "motion edit failed. Reconfirm actor selection and provide concrete ticks, channels, or path points.";
        }

        return "the previous write did not succeed. Inspect the target again and choose a more specific tool call.";
    }

    public static class RequestProfile
    {
        public boolean actionable;
        public boolean broadCreativeRequest;
        public boolean complexRequest;
        public boolean actorFocused;
        public boolean formFocused;
        public boolean cameraFocused;
        public boolean assetFocused;
        public boolean hasRecentQuestionCard;
        public boolean hasRecentTodoCard;
        public boolean hasRecentSceneInfo;
        public boolean hasRecentActorList;
        public boolean hasRecentAssetDiscovery;
        public boolean hasRecentFormInspect;
        public boolean hasRecentCameraInspect;
        public boolean hasRecentPreview;
        public boolean hasRecentActorMotionWrite;
        public boolean hasRecentFormWrite;
        public boolean hasRecentCameraWrite;
        public AISession.WorkflowStage workflowStage;
        public String workflowHint;
        public String workingMemorySummary;
    }

    public static class ToolFailure
    {
        public final String toolName;
        public final String message;

        public ToolFailure(String toolName, String message)
        {
            this.toolName = toolName;
            this.message = message == null ? "" : message;
        }
    }
}
