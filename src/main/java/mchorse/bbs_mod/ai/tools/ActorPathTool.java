package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.base.BaseValue;

public class ActorPathTool implements IAITool
{
    @Override
    public String getName()
    {
        return "actor_path";
    }

    @Override
    public String getDescription()
    {
        return "Build a smooth actor movement path by inserting many x/y/z/yaw/pitch/body/head keyframes with interpolation.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"actor_index\":{\"type\":\"integer\"}," +
            "\"points\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{" +
            "\"tick\":{\"type\":\"integer\"},\"x\":{\"type\":\"number\"},\"y\":{\"type\":\"number\"},\"z\":{\"type\":\"number\"}," +
            "\"yaw\":{\"type\":\"number\"},\"pitch\":{\"type\":\"number\"},\"head_yaw\":{\"type\":\"number\"},\"body_yaw\":{\"type\":\"number\"}," +
            "\"interpolation\":{\"type\":\"string\"}" +
            "},\"required\":[\"tick\"]}}," +
            "\"looping\":{\"type\":\"integer\"}" +
            "},\"required\":[\"actor_index\",\"points\"]}";
    }

    @Override
    public AIToolResult execute(String argumentsJson, AIToolContext context)
    {
        Film film = context.getFilm();

        if (film == null)
        {
            return AIToolResult.error("No film is currently open.");
        }

        try
        {
            JsonObject args = JsonParser.parseString(argumentsJson).getAsJsonObject();
            int actorIndex = args.get("actor_index").getAsInt();
            JsonArray points = args.getAsJsonArray("points");
            int looping = args.has("looping") ? args.get("looping").getAsInt() : -1;

            Replay replay = AIToolUtils.getReplay(film, actorIndex);

            if (replay == null)
            {
                return AIToolResult.error(AIToolUtils.replayOutOfRangeMessage(film, actorIndex));
            }

            final int[] count = {0};

            BaseValue.edit(film, (f) ->
            {
                for (JsonElement element : points)
                {
                    JsonObject point = element.getAsJsonObject();
                    int tick = point.get("tick").getAsInt();

                    if (point.has("x")) count[0] += insert(replay.keyframes.x, tick, point.get("x").getAsDouble(), point, null);
                    if (point.has("y")) count[0] += insert(replay.keyframes.y, tick, point.get("y").getAsDouble(), point, null);
                    if (point.has("z")) count[0] += insert(replay.keyframes.z, tick, point.get("z").getAsDouble(), point, null);
                    if (point.has("yaw")) count[0] += insert(replay.keyframes.yaw, tick, point.get("yaw").getAsDouble(), point, null);
                    if (point.has("pitch")) count[0] += insert(replay.keyframes.pitch, tick, point.get("pitch").getAsDouble(), point, null);
                    if (point.has("head_yaw")) count[0] += insert(replay.keyframes.headYaw, tick, point.get("head_yaw").getAsDouble(), point, null);
                    if (point.has("body_yaw")) count[0] += insert(replay.keyframes.bodyYaw, tick, point.get("body_yaw").getAsDouble(), point, null);
                }

                if (looping >= 0)
                {
                    replay.looping.set(looping);
                }
            });

            return AIToolResult.success("Inserted " + count[0] + " path keyframe values on actor \"" + replay.getName() + "\".", count[0]);
        }
        catch (Exception e)
        {
            return AIToolResult.error("Actor path failed: " + e.getMessage());
        }
    }

    private int insert(mchorse.bbs_mod.utils.keyframes.KeyframeChannel<Double> channel, int tick, double value, JsonObject point, String field)
    {
        int index = channel.insert(tick, value);

        return 1 + AIToolUtils.applyInterpolation(channel, index, point, field);
    }
}
