package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.ai.AISettings;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
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
        return "Create animations by inserting multiple keyframes at once on an actor. Can create looping/repetitive animations (walk, idle, run, fight, jump). " +
            "Provide an array of keyframes with tick, and channel values. Automatically detects and removes duplicate/overlapping keyframes. " +
            "Enforces a maximum of " + AISettings.maxKeyframesPerOperation.get() + " keyframes per operation.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"actor_index\":{\"type\":\"integer\",\"description\":\"Index of the actor\"}," +
            "\"keyframes\":{\"type\":\"array\",\"description\":\"Array of keyframe objects\",\"items\":{\"type\":\"object\",\"properties\":{" +
            "\"tick\":{\"type\":\"integer\",\"description\":\"Tick position\"}," +
            "\"x\":{\"type\":\"number\"},\"y\":{\"type\":\"number\"},\"z\":{\"type\":\"number\"}," +
            "\"yaw\":{\"type\":\"number\"},\"pitch\":{\"type\":\"number\"}," +
            "\"head_yaw\":{\"type\":\"number\"},\"body_yaw\":{\"type\":\"number\"}," +
            "\"sneaking\":{\"type\":\"number\"},\"sprinting\":{\"type\":\"number\"}," +
            "\"interpolation\":{\"type\":\"string\",\"description\":\"Optional interpolation for values inserted at this point\"}," +
            "\"interpolation_args\":{\"type\":\"array\",\"items\":{\"type\":\"number\"}}," +
            "\"duration\":{\"type\":\"number\"}," +
            "\"lx\":{\"type\":\"number\"},\"ly\":{\"type\":\"number\"},\"rx\":{\"type\":\"number\"},\"ry\":{\"type\":\"number\"}" +
            "},\"required\":[\"tick\"]}}," +
            "\"looping\":{\"type\":\"integer\",\"description\":\"If set, enables looping at this tick count (0 = no loop)\"}," +
            "\"clear_existing\":{\"type\":\"boolean\",\"description\":\"If true, clear all existing keyframes on affected channels before inserting\"}" +
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
            int maxKf = AISettings.maxKeyframesPerOperation.get();

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
                    replay.keyframes.yaw.removeAll();
                    replay.keyframes.pitch.removeAll();
                    replay.keyframes.headYaw.removeAll();
                    replay.keyframes.bodyYaw.removeAll();
                    replay.keyframes.sneaking.removeAll();
                    replay.keyframes.sprinting.removeAll();
                }

                for (JsonElement element : keyframesArray)
                {
                    JsonObject kf = element.getAsJsonObject();
                    int tick = kf.get("tick").getAsInt();
                    if (tick < 0) continue;

                    insertCount[0] += insert(replay.keyframes.x, tick, kf, "x");
                    insertCount[0] += insert(replay.keyframes.y, tick, kf, "y");
                    insertCount[0] += insert(replay.keyframes.z, tick, kf, "z");
                    insertCount[0] += insert(replay.keyframes.yaw, tick, kf, "yaw");
                    insertCount[0] += insert(replay.keyframes.pitch, tick, kf, "pitch");
                    insertCount[0] += insert(replay.keyframes.headYaw, tick, kf, "head_yaw");
                    insertCount[0] += insert(replay.keyframes.bodyYaw, tick, kf, "body_yaw");
                    insertCount[0] += insert(replay.keyframes.sneaking, tick, kf, "sneaking");
                    insertCount[0] += insert(replay.keyframes.sprinting, tick, kf, "sprinting");
                }

                /* Set looping if specified */
                if (looping >= 0)
                {
                    replay.looping.set(looping);
                }

                /* Simplify channels to remove redundant keyframes */
                replay.keyframes.x.simplify();
                replay.keyframes.y.simplify();
                replay.keyframes.z.simplify();
                replay.keyframes.yaw.simplify();
                replay.keyframes.pitch.simplify();
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

    private int insert(KeyframeChannel<Double> channel, int tick, JsonObject json, String key)
    {
        if (!json.has(key))
        {
            return 0;
        }

        int index = channel.insert(tick, json.get(key).getAsDouble());
        AIToolUtils.applyKeyframeMeta(channel, index, json);

        return 1;
    }
}
