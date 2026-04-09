package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.base.BaseValue;

import java.util.List;

/**
 * Sets position, rotation, and scale keyframes on an actor at a specific tick.
 */
public class ActionTool implements IAITool
{
    @Override
    public String getName()
    {
        return "action_tool";
    }

    @Override
    public String getDescription()
    {
        return "Set actor keyframes at a specific tick, with optional interpolation for smoother motion.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"actor_index\":{\"type\":\"integer\",\"description\":\"Index of the actor (from actor_list)\"}," +
            "\"tick\":{\"type\":\"integer\",\"description\":\"Tick to set the keyframe at\"}," +
            "\"x\":{\"type\":\"number\",\"description\":\"X position\"}," +
            "\"y\":{\"type\":\"number\",\"description\":\"Y position\"}," +
            "\"z\":{\"type\":\"number\",\"description\":\"Z position\"}," +
            "\"yaw\":{\"type\":\"number\",\"description\":\"Yaw rotation in degrees\"}," +
            "\"pitch\":{\"type\":\"number\",\"description\":\"Pitch rotation in degrees\"}," +
            "\"head_yaw\":{\"type\":\"number\",\"description\":\"Head yaw rotation\"}," +
            "\"body_yaw\":{\"type\":\"number\",\"description\":\"Body yaw rotation\"}," +
            "\"sneaking\":{\"type\":\"boolean\",\"description\":\"Whether actor is sneaking\"}," +
            "\"sprinting\":{\"type\":\"boolean\",\"description\":\"Whether actor is sprinting\"}," +
            "\"interpolation\":{\"type\":\"string\",\"description\":\"Optional interpolation for all inserted keys\"}" +
            "},\"required\":[\"actor_index\",\"tick\"]}";
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
            int tick = args.get("tick").getAsInt();

            if (tick < 0) return AIToolResult.error("Tick must be >= 0.");

            List<Replay> replays = film.replays.getList();
            if (actorIndex < 0 || actorIndex >= replays.size())
            {
                return AIToolResult.error("Actor index " + actorIndex + " is out of range. Use actor_list to see valid indices.");
            }

            Replay replay = replays.get(actorIndex);

            final int[] changeCount = {0};

            BaseValue.edit(film, (f) ->
            {
                if (args.has("x")) { changeCount[0] += insert(replay.keyframes.x, tick, args.get("x").getAsDouble(), args); }
                if (args.has("y")) { changeCount[0] += insert(replay.keyframes.y, tick, args.get("y").getAsDouble(), args); }
                if (args.has("z")) { changeCount[0] += insert(replay.keyframes.z, tick, args.get("z").getAsDouble(), args); }
                if (args.has("yaw")) { changeCount[0] += insert(replay.keyframes.yaw, tick, args.get("yaw").getAsDouble(), args); }
                if (args.has("pitch")) { changeCount[0] += insert(replay.keyframes.pitch, tick, args.get("pitch").getAsDouble(), args); }
                if (args.has("head_yaw")) { changeCount[0] += insert(replay.keyframes.headYaw, tick, args.get("head_yaw").getAsDouble(), args); }
                if (args.has("body_yaw")) { changeCount[0] += insert(replay.keyframes.bodyYaw, tick, args.get("body_yaw").getAsDouble(), args); }
                if (args.has("sneaking")) { changeCount[0] += insert(replay.keyframes.sneaking, tick, args.get("sneaking").getAsBoolean() ? 1.0 : 0.0, args); }
                if (args.has("sprinting")) { changeCount[0] += insert(replay.keyframes.sprinting, tick, args.get("sprinting").getAsBoolean() ? 1.0 : 0.0, args); }
            });

            return AIToolResult.success(
                "Set " + changeCount[0] + " keyframe(s) on actor \"" + replay.getName() + "\" at tick " + tick,
                changeCount[0]
            );
        }
        catch (Exception e)
        {
            return AIToolResult.error("Failed to set keyframes: " + e.getMessage());
        }
    }

    private int insert(mchorse.bbs_mod.utils.keyframes.KeyframeChannel<Double> channel, int tick, double value, JsonObject args)
    {
        int index = channel.insert(tick, value);

        return 1 + AIToolUtils.applyInterpolation(channel, index, args, null);
    }
}
