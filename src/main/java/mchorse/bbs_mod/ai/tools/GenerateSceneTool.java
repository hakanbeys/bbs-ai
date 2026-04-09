package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.camera.clips.overwrite.KeyframeClip;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.settings.values.base.BaseValue;

import java.util.List;

/**
 * Generates a complete cinematic scene from a text prompt.
 * Creates multiple actors with full animation keyframes and camera clips.
 * This is the "one-shot" scene generator tool — the AI describes the full scene
 * as structured JSON and this tool builds it in one call.
 */
public class GenerateSceneTool implements IAITool
{
    @Override
    public String getName()
    {
        return "generate_scene";
    }

    @Override
    public String getDescription()
    {
        return "Generate a complete cinematic scene in one call. Creates multiple actors with full animation keyframes " +
            "and camera clips with movement. Actors support ALL keyframe channels: x/y/z (position), vX/vY/vZ (velocity), " +
            "yaw/pitch (rotation), head_yaw/body_yaw (head/body twist), sneaking/sprinting/grounded (states), " +
            "stick_lx/stick_ly (left analog), stick_rx/stick_ry (right analog), trigger_l/trigger_r, " +
            "extra1_x/extra1_y/extra2_x/extra2_y. Camera clips have x/y/z/yaw/pitch/roll/fov. " +
            "Tick rate: 20/second. Y=64 is ground. " +
            "CRITICAL: Use 15-30+ keyframes per actor for SMOOTH motion. Always include camera clips. " +
            "Set looping > 0 for cyclical animations (walk=40, run=20, idle=60).";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"scene_name\":{\"type\":\"string\",\"description\":\"Name for this scene\"}," +
            "\"duration\":{\"type\":\"integer\",\"description\":\"Total scene duration in ticks (20 = 1 second)\"}," +
            "\"actors\":{\"type\":\"array\",\"description\":\"Array of actor definitions\",\"items\":{\"type\":\"object\",\"properties\":{" +
            "\"label\":{\"type\":\"string\",\"description\":\"Actor display name\"}," +
            "\"model_path\":{\"type\":\"string\",\"description\":\"Model path (optional, e.g. bbs:models/mymodel.bbmodel)\"}," +
            "\"looping\":{\"type\":\"integer\",\"description\":\"Loop point in ticks. 0 = no loop.\"}," +
            "\"keyframes\":{\"type\":\"array\",\"description\":\"Array of keyframe objects with tick and channel values\",\"items\":{\"type\":\"object\",\"properties\":{" +
            "\"tick\":{\"type\":\"integer\"}," +
            "\"x\":{\"type\":\"number\"},\"y\":{\"type\":\"number\"},\"z\":{\"type\":\"number\"}," +
            "\"yaw\":{\"type\":\"number\"},\"pitch\":{\"type\":\"number\"}," +
            "\"head_yaw\":{\"type\":\"number\"},\"body_yaw\":{\"type\":\"number\"}," +
            "\"sneaking\":{\"type\":\"number\"},\"sprinting\":{\"type\":\"number\"}" +
            "}}}" +
            "},\"required\":[\"label\",\"keyframes\"]}}," +
            "\"camera\":{\"type\":\"array\",\"description\":\"Array of camera clip definitions (KeyframeClip type)\",\"items\":{\"type\":\"object\",\"properties\":{" +
            "\"title\":{\"type\":\"string\",\"description\":\"Camera clip name\"}," +
            "\"tick\":{\"type\":\"integer\",\"description\":\"Start tick of this camera clip\"}," +
            "\"duration\":{\"type\":\"integer\",\"description\":\"Duration of this clip in ticks\"}," +
            "\"keyframes\":{\"type\":\"array\",\"description\":\"Camera position/rotation keyframes within this clip (relative to clip start)\",\"items\":{\"type\":\"object\",\"properties\":{" +
            "\"tick\":{\"type\":\"integer\",\"description\":\"Tick relative to clip start\"}," +
            "\"x\":{\"type\":\"number\"},\"y\":{\"type\":\"number\"},\"z\":{\"type\":\"number\"}," +
            "\"yaw\":{\"type\":\"number\"},\"pitch\":{\"type\":\"number\"},\"roll\":{\"type\":\"number\"}," +
            "\"fov\":{\"type\":\"number\",\"description\":\"Field of view in degrees (default ~70)\"}" +
            "}}}" +
            "}}}" +
            "},\"required\":[\"actors\"]}";
    }

    @Override
    public AIToolResult execute(String argumentsJson, AIToolContext context)
    {
        Film film = context.getFilm();
        if (film == null) return AIToolResult.error("No film is currently open.");

        try
        {
            JsonObject args = JsonParser.parseString(argumentsJson).getAsJsonObject();
            JsonArray actorsArray = args.getAsJsonArray("actors");

            if (actorsArray == null || actorsArray.isEmpty())
            {
                return AIToolResult.error("At least one actor is required in the 'actors' array.");
            }

            final int[] totalChanges = {0};

            BaseValue.edit(film, (f) ->
            {
                /* === Create actors === */
                for (JsonElement actorEl : actorsArray)
                {
                    JsonObject actorDef = actorEl.getAsJsonObject();
                    String label = actorDef.has("label") ? actorDef.get("label").getAsString() : "Actor";
                    int looping = actorDef.has("looping") ? actorDef.get("looping").getAsInt() : 0;
                    String modelPath = actorDef.has("model_path") ? actorDef.get("model_path").getAsString() : "";

                    Replay replay = f.replays.addReplay();
                    replay.label.set(label);
                    replay.looping.set(looping);

                    /* Assign a ModelForm if model specified or just a default form */
                    ModelForm modelForm = new ModelForm();
                    modelForm.animatable.set(true);
                    if (!modelPath.isEmpty())
                    {
                        modelForm.model.set(modelPath);
                    }
                    replay.form.set(modelForm);
                    totalChanges[0]++;

                    /* Insert keyframes */
                    JsonArray keyframes = actorDef.getAsJsonArray("keyframes");
                    if (keyframes != null)
                    {
                        for (JsonElement kfEl : keyframes)
                        {
                            JsonObject kf = kfEl.getAsJsonObject();
                            int tick = kf.get("tick").getAsInt();
                            if (tick < 0) continue;

                            /* Position */
                            if (kf.has("x")) { replay.keyframes.x.insert(tick, kf.get("x").getAsDouble()); totalChanges[0]++; }
                            if (kf.has("y")) { replay.keyframes.y.insert(tick, kf.get("y").getAsDouble()); totalChanges[0]++; }
                            if (kf.has("z")) { replay.keyframes.z.insert(tick, kf.get("z").getAsDouble()); totalChanges[0]++; }

                            /* Velocity */
                            if (kf.has("vX")) { replay.keyframes.vX.insert(tick, kf.get("vX").getAsDouble()); totalChanges[0]++; }
                            if (kf.has("vY")) { replay.keyframes.vY.insert(tick, kf.get("vY").getAsDouble()); totalChanges[0]++; }
                            if (kf.has("vZ")) { replay.keyframes.vZ.insert(tick, kf.get("vZ").getAsDouble()); totalChanges[0]++; }

                            /* Rotation */
                            if (kf.has("yaw")) { replay.keyframes.yaw.insert(tick, kf.get("yaw").getAsDouble()); totalChanges[0]++; }
                            if (kf.has("pitch")) { replay.keyframes.pitch.insert(tick, kf.get("pitch").getAsDouble()); totalChanges[0]++; }
                            if (kf.has("head_yaw")) { replay.keyframes.headYaw.insert(tick, kf.get("head_yaw").getAsDouble()); totalChanges[0]++; }
                            if (kf.has("body_yaw")) { replay.keyframes.bodyYaw.insert(tick, kf.get("body_yaw").getAsDouble()); totalChanges[0]++; }

                            /* States */
                            if (kf.has("sneaking")) { replay.keyframes.sneaking.insert(tick, kf.get("sneaking").getAsDouble()); totalChanges[0]++; }
                            if (kf.has("sprinting")) { replay.keyframes.sprinting.insert(tick, kf.get("sprinting").getAsDouble()); totalChanges[0]++; }
                            if (kf.has("grounded")) { replay.keyframes.grounded.insert(tick, kf.get("grounded").getAsDouble()); totalChanges[0]++; }
                            if (kf.has("fall")) { replay.keyframes.fall.insert(tick, kf.get("fall").getAsDouble()); totalChanges[0]++; }
                            if (kf.has("damage")) { replay.keyframes.damage.insert(tick, kf.get("damage").getAsDouble()); totalChanges[0]++; }

                            /* Analog sticks */
                            if (kf.has("stick_lx")) { replay.keyframes.stickLeftX.insert(tick, kf.get("stick_lx").getAsDouble()); totalChanges[0]++; }
                            if (kf.has("stick_ly")) { replay.keyframes.stickLeftY.insert(tick, kf.get("stick_ly").getAsDouble()); totalChanges[0]++; }
                            if (kf.has("stick_rx")) { replay.keyframes.stickRightX.insert(tick, kf.get("stick_rx").getAsDouble()); totalChanges[0]++; }
                            if (kf.has("stick_ry")) { replay.keyframes.stickRightY.insert(tick, kf.get("stick_ry").getAsDouble()); totalChanges[0]++; }

                            /* Triggers */
                            if (kf.has("trigger_l")) { replay.keyframes.triggerLeft.insert(tick, kf.get("trigger_l").getAsDouble()); totalChanges[0]++; }
                            if (kf.has("trigger_r")) { replay.keyframes.triggerRight.insert(tick, kf.get("trigger_r").getAsDouble()); totalChanges[0]++; }

                            /* Extras */
                            if (kf.has("extra1_x")) { replay.keyframes.extra1X.insert(tick, kf.get("extra1_x").getAsDouble()); totalChanges[0]++; }
                            if (kf.has("extra1_y")) { replay.keyframes.extra1Y.insert(tick, kf.get("extra1_y").getAsDouble()); totalChanges[0]++; }
                            if (kf.has("extra2_x")) { replay.keyframes.extra2X.insert(tick, kf.get("extra2_x").getAsDouble()); totalChanges[0]++; }
                            if (kf.has("extra2_y")) { replay.keyframes.extra2Y.insert(tick, kf.get("extra2_y").getAsDouble()); totalChanges[0]++; }
                        }
                    }
                }

                /* === Create camera clips === */
                JsonArray cameraArray = args.has("camera") ? args.getAsJsonArray("camera") : null;
                if (cameraArray != null)
                {
                    for (JsonElement camEl : cameraArray)
                    {
                        JsonObject camDef = camEl.getAsJsonObject();
                        KeyframeClip clip = new KeyframeClip();

                        if (camDef.has("title")) clip.title.set(camDef.get("title").getAsString());
                        if (camDef.has("tick")) clip.tick.set(camDef.get("tick").getAsInt());
                        if (camDef.has("duration")) clip.duration.set(camDef.get("duration").getAsInt());

                        /* Insert camera keyframes */
                        JsonArray camKfs = camDef.has("keyframes") ? camDef.getAsJsonArray("keyframes") : null;
                        if (camKfs != null)
                        {
                            for (JsonElement ckfEl : camKfs)
                            {
                                JsonObject ckf = ckfEl.getAsJsonObject();
                                int t = ckf.has("tick") ? ckf.get("tick").getAsInt() : 0;

                                if (ckf.has("x")) clip.x.insert(t, ckf.get("x").getAsDouble());
                                if (ckf.has("y")) clip.y.insert(t, ckf.get("y").getAsDouble());
                                if (ckf.has("z")) clip.z.insert(t, ckf.get("z").getAsDouble());
                                if (ckf.has("yaw")) clip.yaw.insert(t, ckf.get("yaw").getAsDouble());
                                if (ckf.has("pitch")) clip.pitch.insert(t, ckf.get("pitch").getAsDouble());
                                if (ckf.has("roll")) clip.roll.insert(t, ckf.get("roll").getAsDouble());
                                if (ckf.has("fov")) clip.fov.insert(t, ckf.get("fov").getAsDouble());
                            }
                        }

                        f.camera.addClip(clip);
                        totalChanges[0]++;
                    }
                }
            });

            /* Build result summary */
            int actorCount = actorsArray.size();
            int cameraCount = args.has("camera") ? args.getAsJsonArray("camera").size() : 0;
            String sceneName = args.has("scene_name") ? args.get("scene_name").getAsString() : "Generated Scene";

            return AIToolResult.success(
                "Generated scene \"" + sceneName + "\": " + actorCount + " actor(s), " +
                cameraCount + " camera clip(s), " + totalChanges[0] + " total changes.",
                totalChanges[0]
            );
        }
        catch (Exception e)
        {
            return AIToolResult.error("Scene generation failed: " + e.getMessage());
        }
    }
}
