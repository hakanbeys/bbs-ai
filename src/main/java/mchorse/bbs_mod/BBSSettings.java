package mchorse.bbs_mod;

import mchorse.bbs_mod.settings.SettingsBuilder;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.settings.values.ui.ValueColors;
import mchorse.bbs_mod.settings.values.ui.ValueEditorLayout;
import mchorse.bbs_mod.settings.values.ui.ValueLanguage;
import mchorse.bbs_mod.settings.values.ui.ValueOnionSkin;
import mchorse.bbs_mod.settings.values.ui.ValueStringKeys;
import mchorse.bbs_mod.settings.values.ui.ValueVideoSettings;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.HashSet;

public class BBSSettings
{
    public static ValueColors favoriteColors;
    public static ValueStringKeys disabledSheets;
    public static ValueLanguage language;
    public static ValueInt primaryColor;
    public static ValueBoolean enableTrackpadIncrements;
    public static ValueBoolean enableTrackpadScrolling;
    public static ValueInt userIntefaceScale;
    public static ValueInt tooltipStyle;
    public static ValueFloat fov;
    public static ValueBoolean hsvColorPicker;
    public static ValueBoolean forceQwerty;
    public static ValueBoolean freezeModels;
    public static ValueFloat axesScale;
    public static ValueBoolean uniformScale;
    public static ValueBoolean clickSound;
    public static ValueBoolean gizmos;

    public static ValueBoolean enableCursorRendering;
    public static ValueBoolean enableMouseButtonRendering;
    public static ValueBoolean enableKeystrokeRendering;
    public static ValueInt keystrokeOffset;
    public static ValueInt keystrokeMode;

    public static ValueLink backgroundImage;
    public static ValueInt backgroundColor;

    public static ValueBoolean chromaSkyEnabled;
    public static ValueInt chromaSkyColor;
    public static ValueBoolean chromaSkyTerrain;
    public static ValueFloat chromaSkyBillboard;

    public static ValueInt scrollbarShadow;
    public static ValueInt scrollbarWidth;
    public static ValueFloat scrollingSensitivity;
    public static ValueFloat scrollingSensitivityHorizontal;
    public static ValueBoolean scrollingSmoothness;

    public static ValueBoolean multiskinMultiThreaded;

    public static ValueString videoEncoderPath;
    public static ValueBoolean videoEncoderLog;
    public static ValueVideoSettings videoSettings;

    public static ValueFloat editorCameraSpeed;
    public static ValueFloat editorCameraAngleSpeed;
    public static ValueInt duration;
    public static ValueBoolean editorLoop;
    public static ValueInt editorJump;
    public static ValueInt editorGuidesColor;
    public static ValueBoolean editorRuleOfThirds;
    public static ValueBoolean editorCenterLines;
    public static ValueBoolean editorCrosshair;
    public static ValueBoolean editorSeconds;
    public static ValueInt editorPeriodicSave;
    public static ValueBoolean editorHorizontalFlight;
    public static ValueEditorLayout editorLayoutSettings;
    public static ValueOnionSkin editorOnionSkin;
    public static ValueBoolean editorSnapToMarkers;
    public static ValueBoolean editorClipPreview;
    public static ValueBoolean editorRewind;
    public static ValueBoolean editorHorizontalClipEditor;
    public static ValueBoolean editorMinutesBackup;

    public static ValueFloat recordingCountdown;
    public static ValueBoolean recordingSwipeDamage;
    public static ValueBoolean recordingOverlays;
    public static ValueInt recordingPoseTransformOverlays;
    public static ValueBoolean recordingCameraPreview;

    public static ValueBoolean renderAllModelBlocks;
    public static ValueBoolean clickModelBlocks;

    public static ValueString entitySelectorsPropertyWhitelist;

    public static ValueBoolean damageControl;

    public static ValueBoolean shaderCurvesEnabled;

    public static ValueBoolean audioWaveformVisible;
    public static ValueInt audioWaveformDensity;
    public static ValueFloat audioWaveformWidth;
    public static ValueInt audioWaveformHeight;
    public static ValueBoolean audioWaveformFilename;
    public static ValueBoolean audioWaveformTime;

    public static ValueString cdnUrl;
    public static ValueString cdnToken;

    /* AI Animator */
    public static ValueString aiProvider;
    public static ValueString aiModel;
    public static ValueString aiApiKey;
    public static ValueString aiBaseUrl;
    public static ValueString aiWebSearchProvider;
    public static ValueString aiWebSearchApiKey;
    public static ValueString aiSystemPrompt;
    public static ValueInt aiMaxKeyframes;
    public static ValueBoolean aiAutoSave;
    public static ValueBoolean aiRequireApproval;

    public static final String DEFAULT_AI_SYSTEM_PROMPT =
        "You are an expert AI Animator inside BBS (BlockBuster Studio) mod for Minecraft. " +
        "You only use actors, models, textures, particles, audio, property paths, and camera data that already exist in the current project. Never invent model keys, asset paths, or form property paths before inspecting them. " +
        "You create professional-quality cinematic animations with smooth motion, dynamic camera work, and compelling choreography. " +
        "20 ticks = 1 second. Minecraft world Y=64 is ground level. " +
        "\n\n" +
        "=== CRITICAL RULES ===" +
        "\n" +
        "0. For broad or underspecified creative requests, ask a compact follow-up using a question card instead of guessing. Use this exact format with no code fence: <question-card title=\"Shot direction\" multi=\"false\">One short question line.\n- Option one\n- Option two\n</question-card>. If multiple choices can be combined, set multi=\"true\"." +
        "\n" +
        "1. NEVER stop after just creating actors. You MUST add keyframes AND camera clips. A scene without camera is INCOMPLETE." +
        "\n" +
        "2. ALWAYS use many keyframes (15-30+) per actor for smooth motion. Sparse keyframes = jerky animation." +
        "\n" +
        "3. ALWAYS create camera work with multiple clips (wide shot -> medium -> close-up -> etc)." +
        "\n" +
        "3.5. For complex multi-step jobs, you may show a compact todo card before acting. Use this exact format: <todo-card title=\"Animation plan\">\n[>] Active step\n[ ] Next step\n[x] Completed step\n</todo-card>. Only use todo cards when they help." +
        "\n" +
        "4. Use generate_scene for complete scenes, or replay_tool + camera_tool for adding to existing scenes." +
        "\n" +
        "5. Every change goes through undo/redo. Keep text responses short — let the animation speak." +
        "\n\n" +
        "=== WORKFLOW ===" +
        "\n" +
        "For NEW scenes: scene_info -> actor_list -> asset_search/model_search -> actor_form -> actor_path/replay_tool -> camera_shot" +
        "\n" +
        "For EXISTING scenes: actor_list -> model_search or asset_search -> actor_form -> form_inspect/form_property_edit/form_property_keyframes -> actor_path/replay_tool -> camera_inspect/camera_clear/camera_shot" +
        "\n" +
        "For MODEL properties: form_inspect -> form_property_edit or form_property_keyframes. Use exact property paths from form_inspect." +
        "\n\n" +
        "=== REPLAY KEYFRAME CHANNELS ===" +
        "\n" +
        "Position: x, y, z (world coordinates, Y=64 is ground)" +
        "\n" +
        "Rotation: yaw (body facing 0-360), pitch (head up/down -90 to 90)" +
        "\n" +
        "Head: head_yaw (head turn relative to body), body_yaw (torso twist)" +
        "\n" +
        "Movement state: sneaking (0/1), sprinting (0/1)" +
        "\n" +
        "Velocity: vX, vY, vZ (entity velocity — useful for particles/physics)" +
        "\n" +
        "Analog sticks: stick_lx, stick_ly (-1 to 1), stick_rx, stick_ry (-1 to 1)" +
        "\n" +
        "Triggers: trigger_l, trigger_r (0 to 1)" +
        "\n" +
        "Extra: extra1_x, extra1_y, extra2_x, extra2_y (custom animatable values)" +
        "\n" +
        "Equipment: item_main_hand, item_off_hand, item_head/chest/legs/feet (ItemStack), selected_slot (0-8)" +
        "\n" +
        "Other: grounded (0/1), fall (fall distance), damage (hurt timer)" +
        "\n" +
        "Looping: Set replay looping > 0 to make animation repeat every N ticks." +
        "\n\n" +
        "=== CAMERA CLIP TYPES ===" +
        "\n" +
        "KeyframeClip (bbs:keyframe) — PRIMARY. Per-channel keyframe animation: x, y, z, yaw, pitch, roll, fov, distance. " +
        "Each channel has independent keyframes with custom easing. Ticks inside clips are RELATIVE to clip start." +
        "\n" +
        "IdleClip (bbs:idle) — Static camera at a fixed position." +
        "\n" +
        "DollyClip (bbs:dolly) — Camera moves along look direction with interpolation." +
        "\n" +
        "PathClip (bbs:path) — Smooth spline path through multiple control points (Hermite interpolation)." +
        "\n\n" +
        "Camera MODIFIERS (layered on top of main clips):" +
        "\n" +
        "TranslateClip (bbs:translate) — Offset camera position XYZ." +
        "\n" +
        "AngleClip (bbs:angle) — Offset camera rotation yaw/pitch/roll/fov." +
        "\n" +
        "DragClip (bbs:drag) — Smooth follow/lag effect (factor 0-1, rate)." +
        "\n" +
        "ShakeClip (bbs:shake) — Camera shake (period + amount). Great for impacts." +
        "\n" +
        "MathClip (bbs:math) — Mathematical expressions for procedural animation." +
        "\n" +
        "LookClip (bbs:look) — Camera looks at entity or block position." +
        "\n" +
        "OrbitClip (bbs:orbit) — Camera orbits around entity (distance, yaw, pitch)." +
        "\n" +
        "RemapperClip (bbs:remapper) — Non-linear time remapping (slow-mo, speed-up)." +
        "\n" +
        "DollyZoomClip (bbs:dolly_zoom) — Hitchcock vertigo zoom effect." +
        "\n" +
        "SubtitleClip (bbs:subtitle) — On-screen text overlay." +
        "\n" +
        "AudioClip (bbs:audio) — Audio playback sync." +
        "\n" +
        "CurveClip (bbs:curve) — Animate shader/world parameters." +
        "\n\n" +
        "=== ACTION CLIPS (Actor actions at specific ticks) ===" +
        "\n" +
        "bbs:swipe — Arm swing animation (attack visual)." +
        "\n" +
        "bbs:attack — Raycast attack dealing damage to hit entity." +
        "\n" +
        "bbs:damage — Direct damage to the actor entity." +
        "\n" +
        "bbs:chat — Broadcast a chat message." +
        "\n" +
        "bbs:command — Execute a server command." +
        "\n" +
        "bbs:place_block — Place or break a block at XYZ." +
        "\n" +
        "bbs:interact_block — Right-click interact with a block." +
        "\n" +
        "bbs:break_block — Show block breaking progress (0-10)." +
        "\n" +
        "bbs:use_item — Use an item (bow draw, eat, etc)." +
        "\n" +
        "bbs:use_block_item — Use item on a block." +
        "\n" +
        "bbs:drop_item — Drop an item entity with velocity." +
        "\n\n" +
        "=== FORM PROPERTIES (animate model bones/transforms) ===" +
        "\n" +
        "Use form_tool 'list' to discover all animatable properties on a model." +
        "\n" +
        "Common properties: visible, lighting, transform (sx/sy/sz scale, tx/ty/tz translate, rx/ry/rz rotate), " +
        "anchor, texture, model, pose, pose_overlay, actions, color, shape_keys." +
        "\n" +
        "Use form_tool 'set' to animate properties with keyframes at specific ticks." +
        "\n\n" +
        "=== SMOOTH ANIMATION TECHNIQUES ===" +
        "\n" +
        "WALK CYCLE (40 ticks, looping=40): Contact-down-pass-up pattern." +
        "\n" +
        "  tick 0: x=0.0, y=64.0, z=0.0, body_yaw=0, head_yaw=0 (contact pose right)" +
        "\n" +
        "  tick 5: x=0.0, y=63.95, z=0.25, body_yaw=-2 (passing right, slight dip)" +
        "\n" +
        "  tick 10: x=0.0, y=64.03, z=0.5, body_yaw=-4, head_yaw=-2 (high point right)" +
        "\n" +
        "  tick 15: x=0.0, y=64.0, z=0.75, body_yaw=-2 (contact left)" +
        "\n" +
        "  tick 20: x=0.0, y=63.95, z=1.0, body_yaw=0 (mid stride)" +
        "\n" +
        "  tick 25: x=0.0, y=63.95, z=1.25, body_yaw=2 (passing left, slight dip)" +
        "\n" +
        "  tick 30: x=0.0, y=64.03, z=1.5, body_yaw=4, head_yaw=2 (high point left)" +
        "\n" +
        "  tick 35: x=0.0, y=64.0, z=1.75, body_yaw=2 (contact right)" +
        "\n" +
        "  tick 40: x=0.0, y=64.0, z=2.0, body_yaw=0, head_yaw=0 (loop point)" +
        "\n\n" +
        "RUN CYCLE (20 ticks, looping=20): Faster, more bounce, larger body_yaw." +
        "\n" +
        "  tick 0: y=64.0, z=0.0, body_yaw=0, sprinting=1 | tick 3: y=64.08, z=0.45, body_yaw=-6" +
        "\n" +
        "  tick 5: y=64.12, z=0.75, body_yaw=-8 | tick 7: y=64.08, z=1.05, body_yaw=-4" +
        "\n" +
        "  tick 10: y=64.0, z=1.5, body_yaw=0 | tick 13: y=64.08, z=1.95, body_yaw=6" +
        "\n" +
        "  tick 15: y=64.12, z=2.25, body_yaw=8 | tick 17: y=64.08, z=2.55, body_yaw=4" +
        "\n" +
        "  tick 20: y=64.0, z=3.0, body_yaw=0" +
        "\n\n" +
        "IDLE BREATHING (60 ticks, looping=60): Very subtle." +
        "\n" +
        "  tick 0: y=64.0, head_yaw=0, pitch=0 | tick 15: y=64.015, head_yaw=1.5, pitch=-0.5" +
        "\n" +
        "  tick 30: y=64.02, head_yaw=0, pitch=0 | tick 45: y=64.015, head_yaw=-1.5, pitch=0.5" +
        "\n" +
        "  tick 60: y=64.0, head_yaw=0, pitch=0" +
        "\n\n" +
        "JUMP (24 ticks): Anticipation-launch-air-land." +
        "\n" +
        "  tick 0: y=64.0, pitch=0 (standing) | tick 4: y=63.9, pitch=5 (squat anticipation)" +
        "\n" +
        "  tick 6: y=64.3, pitch=-5 (launch) | tick 10: y=65.2, pitch=-10 (peak)" +
        "\n" +
        "  tick 14: y=65.0, pitch=-5 (descent) | tick 18: y=64.2, pitch=3 (approaching ground)" +
        "\n" +
        "  tick 20: y=63.9, pitch=8 (impact squat) | tick 24: y=64.0, pitch=0 (recovery)" +
        "\n\n" +
        "FIGHT COMBO (40 ticks): Wind-up -> strike -> follow-through -> recovery." +
        "\n" +
        "  tick 0: yaw=0, body_yaw=0 (stance) | tick 4: body_yaw=-15, head_yaw=5 (wind-up)" +
        "\n" +
        "  tick 6: body_yaw=25, head_yaw=-3, yaw+=5 (fast strike) | tick 8: body_yaw=30 (impact)" +
        "\n" +
        "  tick 12: body_yaw=10 (follow-through) | tick 16: body_yaw=-10 (second wind-up)" +
        "\n" +
        "  tick 18: body_yaw=20, yaw+=3 (second strike) | tick 22: body_yaw=5 (recovery)" +
        "\n" +
        "  tick 26: body_yaw=-20, y-=0.15 (low sweep wind-up) | tick 28: body_yaw=35, yaw+=8 (sweep)" +
        "\n" +
        "  tick 32: body_yaw=15 (sweep follow) | tick 36: body_yaw=0 (returning to stance)" +
        "\n" +
        "  tick 40: yaw=0, body_yaw=0 (stance)" +
        "\n\n" +
        "DRAMATIC ENTRANCE (60 ticks): Actor walks in from distance." +
        "\n" +
        "  tick 0: x=10, y=64, z=-5, yaw=180 (far away) | tick 20: x=5, y=64.02, z=-2.5, yaw=170" +
        "\n" +
        "  tick 40: x=1, y=64, z=-0.5, yaw=160, body_yaw=-5 | tick 50: x=0.2, y=64, z=0, yaw=150" +
        "\n" +
        "  tick 55: x=0, y=64, z=0, yaw=180, body_yaw=0 (arrives, faces camera) | tick 60: head_yaw=0, pitch=-3 (looks at camera)" +
        "\n\n" +
        "=== CAMERA CINEMATOGRAPHY ===" +
        "\n" +
        "ESTABLISHING WIDE (60 ticks): Far away, low FOV, slow pan." +
        "\n" +
        "  tick 0: x=15, y=68, z=-10, yaw=-35, pitch=15, fov=40 | tick 60: x=12, y=67, z=-8, yaw=-30, pitch=12, fov=45" +
        "\n\n" +
        "CLOSE-UP (40 ticks): Near actor face, slight movement." +
        "\n" +
        "  tick 0: x=actorX+1.5, y=actorY+1.6, z=actorZ, yaw=90, pitch=5, fov=50" +
        "\n" +
        "  tick 40: x=actorX+1.2, y=actorY+1.65, z=actorZ+0.2, yaw=85, pitch=3, fov=48" +
        "\n\n" +
        "ORBIT SHOT (60 ticks): Camera circles around actor." +
        "\n" +
        "  Compute x=centerX+R*cos(angle), z=centerZ+R*sin(angle), yaw tracks actor. R=4-6 blocks." +
        "\n\n" +
        "LOW ANGLE HERO (40 ticks): Camera below actor, looking up." +
        "\n" +
        "  y=actorY-0.5, pitch=-20 to -30, fov=60-65. Very dramatic." +
        "\n\n" +
        "TRACKING DOLLY (60 ticks): Camera moves parallel to actor movement." +
        "\n" +
        "  Camera x follows actor x with offset, steady y and z." +
        "\n\n" +
        "OVER-THE-SHOULDER (40 ticks): Behind one actor looking at another." +
        "\n" +
        "  x=actor1X-1, y=actor1Y+1.8, z=actor1Z+0.5, yaw facing actor2." +
        "\n\n" +
        "ELASTIC BOUNCE TRICK: Use two form property keyframes close together with different scales to create bouncy deformation." +
        "\n\n" +
        "MULTI-SHOT EDITING: Use 3-5 camera clips per scene. Cut between wide/medium/close-up for dynamic storytelling. " +
        "Each clip should be 20-60 ticks. Avoid staying on one angle for too long.";

    public static int primaryColor()
    {
        return primaryColor(Colors.A50);
    }

    public static int primaryColor(int alpha)
    {
        return primaryColor.get() | alpha;
    }

    public static int getDefaultDuration()
    {
        return duration == null ? 30 : duration.get();
    }

    public static float getFov()
    {
        return BBSSettings.fov == null ? MathUtils.toRad(50) : MathUtils.toRad(BBSSettings.fov.get());
    }

    public static void register(SettingsBuilder builder)
    {
        HashSet<String> defaultFilters = new HashSet<>();

        defaultFilters.add("item_off_hand");
        defaultFilters.add("item_head");
        defaultFilters.add("item_chest");
        defaultFilters.add("item_legs");
        defaultFilters.add("item_feet");
        defaultFilters.add("vX");
        defaultFilters.add("vY");
        defaultFilters.add("vZ");
        defaultFilters.add("grounded");
        defaultFilters.add("stick_rx");
        defaultFilters.add("stick_ry");
        defaultFilters.add("trigger_l");
        defaultFilters.add("trigger_r");
        defaultFilters.add("extra1_x");
        defaultFilters.add("extra1_y");
        defaultFilters.add("extra2_x");
        defaultFilters.add("extra2_y");

        builder.category("appearance");
        builder.register(language = new ValueLanguage("language"));
        primaryColor = builder.getInt("primary_color", Colors.ACTIVE).color();
        enableTrackpadIncrements = builder.getBoolean("trackpad_increments", true);
        enableTrackpadScrolling = builder.getBoolean("trackpad_scrolling", true);
        userIntefaceScale = builder.getInt("ui_scale", 2, 0, 4);
        tooltipStyle = builder.getInt("tooltip_style", 1);
        fov = builder.getFloat("fov", 40, 0, 180);
        hsvColorPicker = builder.getBoolean("hsv_color_picker", true);
        forceQwerty = builder.getBoolean("force_qwerty", false);
        freezeModels = builder.getBoolean("freeze_models", false);
        axesScale = builder.getFloat("axes_scale", 1F, 0F, 2F);
        uniformScale = builder.getBoolean("uniform_scale", false);
        clickSound = builder.getBoolean("click_sound", false);
        gizmos = builder.getBoolean("gizmos", true);
        favoriteColors = new ValueColors("favorite_colors");
        disabledSheets = new ValueStringKeys("disabled_sheets");
        disabledSheets.set(defaultFilters);
        builder.register(favoriteColors);
        builder.register(disabledSheets);

        builder.category("tutorials");
        enableCursorRendering = builder.getBoolean("cursor", false);
        enableMouseButtonRendering = builder.getBoolean("mouse_buttons", false);
        enableKeystrokeRendering = builder.getBoolean("keystrokes", false);
        keystrokeOffset = builder.getInt("keystrokes_offset", 10, 0, 20);
        keystrokeMode = builder.getInt("keystrokes_position", 1);

        builder.category("background");
        backgroundImage = builder.getRL("image", null);
        backgroundColor = builder.getInt("color", Colors.A75).colorAlpha();

        builder.category("chroma_sky");
        chromaSkyEnabled = builder.getBoolean("enabled", false);
        chromaSkyColor = builder.getInt("color", Colors.A75).color();
        chromaSkyTerrain = builder.getBoolean("terrain", true);
        chromaSkyBillboard = builder.getFloat("billboard", 0F, 0F, 256F);

        builder.category("scrollbars");
        scrollbarShadow = builder.getInt("shadow", Colors.A50).colorAlpha();
        scrollbarWidth = builder.getInt("width", 4, 2, 10);
        scrollingSensitivity = builder.getFloat("sensitivity", 1F, 0F, 10F);
        scrollingSensitivityHorizontal = builder.getFloat("sensitivity_horizontal", 1F, 0F, 10F);
        scrollingSmoothness = builder.getBoolean("smoothness", true);

        builder.category("multiskin");
        multiskinMultiThreaded = builder.getBoolean("multithreaded", true);

        builder.category("video");
        videoEncoderPath = builder.getString("encoder_path", "ffmpeg");
        videoEncoderLog = builder.getBoolean("log", true);
        builder.register(videoSettings = new ValueVideoSettings("settings"));

        /* Camera editor */
        builder.category("editor");
        editorCameraSpeed = builder.getFloat("speed", 1F, 0.1F, 100F);
        editorCameraAngleSpeed = builder.getFloat("angle_speed", 1F, 0.1F, 100F);
        duration = builder.getInt("duration", 30, 1, 1000);
        editorJump = builder.getInt("jump", 5, 1, 1000);
        editorLoop = builder.getBoolean("loop", false);
        editorGuidesColor = builder.getInt("guides_color", 0xcccc0000).colorAlpha();
        editorRuleOfThirds = builder.getBoolean("rule_of_thirds", false);
        editorCenterLines = builder.getBoolean("center_lines", false);
        editorCrosshair = builder.getBoolean("crosshair", false);
        editorSeconds = builder.getBoolean("seconds", false);
        editorPeriodicSave = builder.getInt("periodic_save", 60, 0, 3600);
        editorHorizontalFlight = builder.getBoolean("horizontal_flight", false);
        builder.register(editorLayoutSettings = new ValueEditorLayout("layout"));
        builder.register(editorOnionSkin = new ValueOnionSkin("onion_skin"));
        editorSnapToMarkers = builder.getBoolean("snap_to_markers", false);
        editorClipPreview = builder.getBoolean("clip_preview", true);
        editorRewind = builder.getBoolean("rewind", true);
        editorHorizontalClipEditor = builder.getBoolean("horizontal_clip_editor", true);
        editorMinutesBackup = builder.getBoolean("minutes_backup", true);

        builder.category("recording");
        recordingCountdown = builder.getFloat("countdown", 1.5F, 0F, 30F);
        recordingSwipeDamage = builder.getBoolean("swipe_damage", false);
        recordingOverlays = builder.getBoolean("overlays", true);
        recordingPoseTransformOverlays = builder.getInt("pose_transform_overlays", 0, 0, 42);
        recordingCameraPreview = builder.getBoolean("camera_preview", true);

        builder.category("model_blocks");
        renderAllModelBlocks = builder.getBoolean("render_all", true);
        clickModelBlocks = builder.getBoolean("click", true);

        builder.category("entity_selectors");
        entitySelectorsPropertyWhitelist = builder.getString("whitelist", "CustomName,Name");

        builder.category("dc");
        damageControl = builder.getBoolean("enabled", true);

        builder.category("shader_curves");
        shaderCurvesEnabled = builder.getBoolean("enabled", true);

        builder.category("audio");
        audioWaveformVisible = builder.getBoolean("waveform_visible", true);
        audioWaveformDensity = builder.getInt("waveform_density", 20, 10, 100);
        audioWaveformWidth = builder.getFloat("waveform_width", 0.8F, 0F, 1F);
        audioWaveformHeight = builder.getInt("waveform_height", 24, 10, 40);
        audioWaveformFilename = builder.getBoolean("waveform_filename", false);
        audioWaveformTime = builder.getBoolean("waveform_time", false);

        builder.category("cdn");
        cdnUrl = builder.getString("url", "");
        cdnToken = builder.getString("token", "");

        builder.category("ai");
        aiProvider = builder.getString("provider", "anthropic");
        aiModel = builder.getString("model", "claude-sonnet-4-20250514");
        aiApiKey = builder.getString("api_key", "");
        aiBaseUrl = builder.getString("base_url", "");
        aiWebSearchProvider = builder.getString("web_search_provider", "tavily");
        aiWebSearchApiKey = builder.getString("web_search_api_key", "");
        aiSystemPrompt = builder.getString("system_prompt", DEFAULT_AI_SYSTEM_PROMPT);
        aiMaxKeyframes = builder.getInt("max_keyframes", 500, 1, 10000);
        aiAutoSave = builder.getBoolean("auto_save", true);
        aiRequireApproval = builder.getBoolean("require_approval", true);
    }
}
