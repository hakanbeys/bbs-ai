package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

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
        return "Set position, rotation, and state keyframes on an actor at a specific tick. Optional interpolation metadata can make the motion smoother.";
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
            "\"interpolation\":{\"type\":\"string\",\"description\":\"Optional interpolation key, for example linear, hermite, const, step\"}," +
            "\"interpolation_args\":{\"type\":\"array\",\"description\":\"Optional interpolation args array of up to 4 numbers\",\"items\":{\"type\":\"number\"}}," +
            "\"duration\":{\"type\":\"number\",\"description\":\"Optional keyframe hold duration\"}," +
            "\"lx\":{\"type\":\"number\"},\"ly\":{\"type\":\"number\"},\"rx\":{\"type\":\"number\"},\"ry\":{\"type\":\"number\"}" +
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
            int changes = 0;

            final int[] changeCount = {0};

            BaseValue.edit(film, (f) ->
            {
                changeCount[0] += insert(replay.keyframes.x, args, tick, "x");
                changeCount[0] += insert(replay.keyframes.y, args, tick, "y");
                changeCount[0] += insert(replay.keyframes.z, args, tick, "z");
                changeCount[0] += insert(replay.keyframes.yaw, args, tick, "yaw");
                changeCount[0] += insert(replay.keyframes.pitch, args, tick, "pitch");
                changeCount[0] += insert(replay.keyframes.headYaw, args, tick, "head_yaw");
                changeCount[0] += insert(replay.keyframes.bodyYaw, args, tick, "body_yaw");

                if (args.has("sneaking"))
                {
                    int index = replay.keyframes.sneaking.insert(tick, args.get("sneaking").getAsBoolean() ? 1.0 : 0.0);
                    AIToolUtils.applyKeyframeMeta(replay.keyframes.sneaking, index, args);
                    changeCount[0] += 1;
                }

                if (args.has("sprinting"))
                {
                    int index = replay.keyframes.sprinting.insert(tick, args.get("sprinting").getAsBoolean() ? 1.0 : 0.0);
                    AIToolUtils.applyKeyframeMeta(replay.keyframes.sprinting, index, args);
                    changeCount[0] += 1;
                }
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

    private int insert(KeyframeChannel<Double> channel, JsonObject args, int tick, String key)
    {
        if (!args.has(key))
        {
            return 0;
        }

        int index = channel.insert(tick, args.get(key).getAsDouble());
        AIToolUtils.applyKeyframeMeta(channel, index, args);

        return 1;
    }
}
