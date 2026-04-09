package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.ai.AISettings;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import java.util.List;

/**
 * Create looping/repetitive animations like walk, idle, run, fight, jump.
 * Inserts multiple keyframes with pattern-based generation.
 */
public class ReplayTool implements IAITool
{
    @Override
    public String getName()
    {
        return "replay_tool";
    }

    @Override
    public String getDescription()
    {
        return "Create animations by inserting multiple keyframes at once on an actor. " +
            "Supports ALL replay channels: position (x,y,z), velocity (vX,vY,vZ), rotation (yaw,pitch), " +
            "head/body (head_yaw,body_yaw), states (sneaking,sprinting,grounded,fall,damage), " +
            "analog sticks (stick_lx,stick_ly,stick_rx,stick_ry), triggers (trigger_l,trigger_r), " +
            "extras (extra1_x,extra1_y,extra2_x,extra2_y). " +
            "For SMOOTH animations: use 15-30+ keyframes, small incremental changes between ticks. " +
            "Enforces max " + AISettings.maxKeyframesPerOperation().get() + " keyframes per call.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"actor_index\":{\"type\":\"integer\",\"description\":\"Index of the actor\"}," +
            "\"keyframes\":{\"type\":\"array\",\"description\":\"Array of keyframe objects\",\"items\":{\"type\":\"object\",\"properties\":{" +
            "\"tick\":{\"type\":\"integer\",\"description\":\"Tick position (20 ticks = 1 second)\"}," +
            "\"x\":{\"type\":\"number\"},\"y\":{\"type\":\"number\"},\"z\":{\"type\":\"number\"}," +
            "\"vX\":{\"type\":\"number\"},\"vY\":{\"type\":\"number\"},\"vZ\":{\"type\":\"number\"}," +
            "\"yaw\":{\"type\":\"number\"},\"pitch\":{\"type\":\"number\"}," +
            "\"head_yaw\":{\"type\":\"number\"},\"body_yaw\":{\"type\":\"number\"}," +
            "\"sneaking\":{\"type\":\"number\"},\"sprinting\":{\"type\":\"number\"}," +
            "\"grounded\":{\"type\":\"number\"},\"fall\":{\"type\":\"number\"},\"damage\":{\"type\":\"number\"}," +
            "\"stick_lx\":{\"type\":\"number\"},\"stick_ly\":{\"type\":\"number\"}," +
            "\"stick_rx\":{\"type\":\"number\"},\"stick_ry\":{\"type\":\"number\"}," +
            "\"trigger_l\":{\"type\":\"number\"},\"trigger_r\":{\"type\":\"number\"}," +
            "\"extra1_x\":{\"type\":\"number\"},\"extra1_y\":{\"type\":\"number\"}," +
            "\"extra2_x\":{\"type\":\"number\"},\"extra2_y\":{\"type\":\"number\"}," +
            "\"interpolation\":{\"type\":\"string\"}" +
            "},\"required\":[\"tick\"]}}," +
            "\"looping\":{\"type\":\"integer\",\"description\":\"If set, enables looping at this tick count (0 = no loop)\"}," +
            "\"clear_existing\":{\"type\":\"boolean\",\"description\":\"If true, clear all existing keyframes on affected channels before inserting\"}," +
            "\"interpolation\":{\"type\":\"string\",\"description\":\"Default interpolation for inserted keys\"}" +
            "},\"required\":[\"actor_index\",\"keyframes\"]}";
    }

    @Override
    public AIToolResult execute(String argumentsJson, AIToolContext context)
    {
        Film film = context.getFilm();
        if (film == null) return AIToolResult.error("No film is currently open.");

        try
        {
            JsonObject args = JsonParser.parseString(argumentsJson).getAsJsonObject();
            int actorIndex = args.get("actor_index").getAsInt();

            List<Replay> replays = film.replays.getList();
            if (actorIndex < 0 || actorIndex >= replays.size())
            {
                return AIToolResult.error("Actor index " + actorIndex + " is out of range.");
            }

            JsonArray keyframesArray = args.getAsJsonArray("keyframes");
            int maxKf = AISettings.maxKeyframesPerOperation().get();

            if (keyframesArray.size() > maxKf)
            {
                return AIToolResult.error("Too many keyframes: " + keyframesArray.size() + ". Maximum allowed: " + maxKf);
            }

            boolean clearExisting = args.has("clear_existing") && args.get("clear_existing").getAsBoolean();
            int looping = args.has("looping") ? args.get("looping").getAsInt() : -1;

            Replay replay = replays.get(actorIndex);
            final int[] insertCount = {0};

            BaseValue.edit(film, (f) ->
            {
                if (clearExisting)
                {
                    replay.keyframes.x.removeAll();
                    replay.keyframes.y.removeAll();
                    replay.keyframes.z.removeAll();
                    replay.keyframes.vX.removeAll();
                    replay.keyframes.vY.removeAll();
                    replay.keyframes.vZ.removeAll();
                    replay.keyframes.yaw.removeAll();
                    replay.keyframes.pitch.removeAll();
                    replay.keyframes.headYaw.removeAll();
                    replay.keyframes.bodyYaw.removeAll();
                    replay.keyframes.sneaking.removeAll();
                    replay.keyframes.sprinting.removeAll();
                    replay.keyframes.grounded.removeAll();
                    replay.keyframes.fall.removeAll();
                    replay.keyframes.damage.removeAll();
                    replay.keyframes.stickLeftX.removeAll();
                    replay.keyframes.stickLeftY.removeAll();
                    replay.keyframes.stickRightX.removeAll();
                    replay.keyframes.stickRightY.removeAll();
                    replay.keyframes.triggerLeft.removeAll();
                    replay.keyframes.triggerRight.removeAll();
                    replay.keyframes.extra1X.removeAll();
                    replay.keyframes.extra1Y.removeAll();
                    replay.keyframes.extra2X.removeAll();
                    replay.keyframes.extra2Y.removeAll();
                }

                for (JsonElement element : keyframesArray)
                {
                    JsonObject kf = element.getAsJsonObject();
                    int tick = kf.get("tick").getAsInt();
                    if (tick < 0) continue;

                    /* Position */
                    if (kf.has("x")) { insertCount[0] += insert(replay.keyframes.x, tick, kf.get("x").getAsDouble(), kf, args); }
                    if (kf.has("y")) { insertCount[0] += insert(replay.keyframes.y, tick, kf.get("y").getAsDouble(), kf, args); }
                    if (kf.has("z")) { insertCount[0] += insert(replay.keyframes.z, tick, kf.get("z").getAsDouble(), kf, args); }

                    /* Velocity */
                    if (kf.has("vX")) { insertCount[0] += insert(replay.keyframes.vX, tick, kf.get("vX").getAsDouble(), kf, args); }
                    if (kf.has("vY")) { insertCount[0] += insert(replay.keyframes.vY, tick, kf.get("vY").getAsDouble(), kf, args); }
                    if (kf.has("vZ")) { insertCount[0] += insert(replay.keyframes.vZ, tick, kf.get("vZ").getAsDouble(), kf, args); }

                    /* Rotation */
                    if (kf.has("yaw")) { insertCount[0] += insert(replay.keyframes.yaw, tick, kf.get("yaw").getAsDouble(), kf, args); }
                    if (kf.has("pitch")) { insertCount[0] += insert(replay.keyframes.pitch, tick, kf.get("pitch").getAsDouble(), kf, args); }
                    if (kf.has("head_yaw")) { insertCount[0] += insert(replay.keyframes.headYaw, tick, kf.get("head_yaw").getAsDouble(), kf, args); }
                    if (kf.has("body_yaw")) { insertCount[0] += insert(replay.keyframes.bodyYaw, tick, kf.get("body_yaw").getAsDouble(), kf, args); }

                    /* States */
                    if (kf.has("sneaking")) { insertCount[0] += insert(replay.keyframes.sneaking, tick, kf.get("sneaking").getAsDouble(), kf, args); }
                    if (kf.has("sprinting")) { insertCount[0] += insert(replay.keyframes.sprinting, tick, kf.get("sprinting").getAsDouble(), kf, args); }
                    if (kf.has("grounded")) { insertCount[0] += insert(replay.keyframes.grounded, tick, kf.get("grounded").getAsDouble(), kf, args); }
                    if (kf.has("fall")) { insertCount[0] += insert(replay.keyframes.fall, tick, kf.get("fall").getAsDouble(), kf, args); }
                    if (kf.has("damage")) { insertCount[0] += insert(replay.keyframes.damage, tick, kf.get("damage").getAsDouble(), kf, args); }

                    /* Analog sticks */
                    if (kf.has("stick_lx")) { insertCount[0] += insert(replay.keyframes.stickLeftX, tick, kf.get("stick_lx").getAsDouble(), kf, args); }
                    if (kf.has("stick_ly")) { insertCount[0] += insert(replay.keyframes.stickLeftY, tick, kf.get("stick_ly").getAsDouble(), kf, args); }
                    if (kf.has("stick_rx")) { insertCount[0] += insert(replay.keyframes.stickRightX, tick, kf.get("stick_rx").getAsDouble(), kf, args); }
                    if (kf.has("stick_ry")) { insertCount[0] += insert(replay.keyframes.stickRightY, tick, kf.get("stick_ry").getAsDouble(), kf, args); }

                    /* Triggers */
                    if (kf.has("trigger_l")) { insertCount[0] += insert(replay.keyframes.triggerLeft, tick, kf.get("trigger_l").getAsDouble(), kf, args); }
                    if (kf.has("trigger_r")) { insertCount[0] += insert(replay.keyframes.triggerRight, tick, kf.get("trigger_r").getAsDouble(), kf, args); }

                    /* Extras */
                    if (kf.has("extra1_x")) { insertCount[0] += insert(replay.keyframes.extra1X, tick, kf.get("extra1_x").getAsDouble(), kf, args); }
                    if (kf.has("extra1_y")) { insertCount[0] += insert(replay.keyframes.extra1Y, tick, kf.get("extra1_y").getAsDouble(), kf, args); }
                    if (kf.has("extra2_x")) { insertCount[0] += insert(replay.keyframes.extra2X, tick, kf.get("extra2_x").getAsDouble(), kf, args); }
                    if (kf.has("extra2_y")) { insertCount[0] += insert(replay.keyframes.extra2Y, tick, kf.get("extra2_y").getAsDouble(), kf, args); }
                }

                /* Set looping if specified */
                if (looping >= 0)
                {
                    replay.looping.set(looping);
                }
            });

            return AIToolResult.success(
                "Inserted " + insertCount[0] + " keyframe values on actor \"" + replay.getName() + "\"" +
                (looping >= 0 ? " (looping at " + looping + " ticks)" : ""),
                insertCount[0]
            );
        }
        catch (Exception e)
        {
            return AIToolResult.error("Failed to create animation: " + e.getMessage());
        }
    }

    private int insert(mchorse.bbs_mod.utils.keyframes.KeyframeChannel<Double> channel, int tick, double value, JsonObject keyframe, JsonObject root)
    {
        int index = channel.insert(tick, value);
        int applied = AIToolUtils.applyInterpolation(channel, index, keyframe, null);

        if (applied == 0)
        {
            applied = AIToolUtils.applyInterpolation(channel, index, root, null);
        }

        return 1 + applied;
    }
}
