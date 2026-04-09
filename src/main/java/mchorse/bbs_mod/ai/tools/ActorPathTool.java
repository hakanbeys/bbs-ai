package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

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
        return "Create a smooth actor movement path from waypoints. Good for cinematic blocking, entrances, exits, strafes, and smooth walk paths.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"actor_index\":{\"type\":\"integer\",\"description\":\"Actor index from actor_list\"}," +
            "\"points\":{\"type\":\"array\",\"description\":\"Waypoint list for the actor path\",\"items\":{\"type\":\"object\",\"properties\":{" +
            "\"tick\":{\"type\":\"integer\"}," +
            "\"x\":{\"type\":\"number\"},\"y\":{\"type\":\"number\"},\"z\":{\"type\":\"number\"}," +
            "\"yaw\":{\"type\":\"number\"},\"pitch\":{\"type\":\"number\"}," +
            "\"head_yaw\":{\"type\":\"number\"},\"body_yaw\":{\"type\":\"number\"}," +
            "\"interpolation\":{\"type\":\"string\"},\"interpolation_args\":{\"type\":\"array\",\"items\":{\"type\":\"number\"}}," +
            "\"duration\":{\"type\":\"number\"},\"lx\":{\"type\":\"number\"},\"ly\":{\"type\":\"number\"},\"rx\":{\"type\":\"number\"},\"ry\":{\"type\":\"number\"}" +
            "},\"required\":[\"tick\",\"x\",\"y\",\"z\"]}}," +
            "\"clear_existing\":{\"type\":\"boolean\",\"description\":\"Clear transform channels first\"}," +
            "\"auto_face_path\":{\"type\":\"boolean\",\"description\":\"If true, infer yaw/body/head yaw from waypoint direction when not provided\"}," +
            "\"set_sprinting\":{\"type\":\"boolean\",\"description\":\"Optional constant sprinting state across all points\"}," +
            "\"set_sneaking\":{\"type\":\"boolean\",\"description\":\"Optional constant sneaking state across all points\"}" +
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
            Replay replay = AIToolUtils.requireReplay(film, actorIndex);

            if (replay == null)
            {
                return AIToolResult.error("Actor index " + actorIndex + " is out of range. Use actor_list first.");
            }

            JsonArray points = args.getAsJsonArray("points");

            if (points == null || points.size() == 0)
            {
                return AIToolResult.error("No path points were provided.");
            }

            boolean clearExisting = args.has("clear_existing") && args.get("clear_existing").getAsBoolean();
            boolean autoFace = !args.has("auto_face_path") || args.get("auto_face_path").getAsBoolean();
            boolean setSprinting = args.has("set_sprinting");
            boolean sprinting = setSprinting && args.get("set_sprinting").getAsBoolean();
            boolean setSneaking = args.has("set_sneaking");
            boolean sneaking = setSneaking && args.get("set_sneaking").getAsBoolean();
            final int[] changes = {0};

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
                }

                for (int i = 0; i < points.size(); i++)
                {
                    JsonObject point = points.get(i).getAsJsonObject();
                    int tick = point.get("tick").getAsInt();
                    if (tick < 0) continue;

                    changes[0] += insert(replay.keyframes.x, tick, point, "x");
                    changes[0] += insert(replay.keyframes.y, tick, point, "y");
                    changes[0] += insert(replay.keyframes.z, tick, point, "z");
                    changes[0] += insert(replay.keyframes.pitch, tick, point, "pitch");

                    float inferredYaw = inferYaw(points, i);

                    if (point.has("yaw"))
                    {
                        changes[0] += insert(replay.keyframes.yaw, tick, point, "yaw");
                    }
                    else if (autoFace)
                    {
                        JsonObject yawJson = cloneWithValue(point, "yaw", inferredYaw);
                        changes[0] += insert(replay.keyframes.yaw, tick, yawJson, "yaw");
                    }

                    if (point.has("body_yaw"))
                    {
                        changes[0] += insert(replay.keyframes.bodyYaw, tick, point, "body_yaw");
                    }
                    else if (autoFace)
                    {
                        JsonObject bodyJson = cloneWithValue(point, "body_yaw", inferredYaw);
                        changes[0] += insert(replay.keyframes.bodyYaw, tick, bodyJson, "body_yaw");
                    }

                    if (point.has("head_yaw"))
                    {
                        changes[0] += insert(replay.keyframes.headYaw, tick, point, "head_yaw");
                    }
                    else if (autoFace)
                    {
                        JsonObject headJson = cloneWithValue(point, "head_yaw", inferredYaw);
                        changes[0] += insert(replay.keyframes.headYaw, tick, headJson, "head_yaw");
                    }

                    if (setSprinting)
                    {
                        JsonObject sprintJson = cloneWithValue(point, "sprinting", sprinting ? 1.0 : 0.0);
                        changes[0] += insert(replay.keyframes.sprinting, tick, sprintJson, "sprinting");
                    }

                    if (setSneaking)
                    {
                        JsonObject sneakJson = cloneWithValue(point, "sneaking", sneaking ? 1.0 : 0.0);
                        changes[0] += insert(replay.keyframes.sneaking, tick, sneakJson, "sneaking");
                    }
                }

                replay.keyframes.x.simplify();
                replay.keyframes.y.simplify();
                replay.keyframes.z.simplify();
                replay.keyframes.yaw.simplify();
                replay.keyframes.pitch.simplify();
                replay.keyframes.headYaw.simplify();
                replay.keyframes.bodyYaw.simplify();
            });

            if (changes[0] == 0)
            {
                return AIToolResult.error("No actor path keyframes were inserted.");
            }

            return AIToolResult.success("Created/updated a smooth path for actor \"" + replay.getName() + "\" using " + points.size() + " waypoint(s)", changes[0]);
        }
        catch (Exception e)
        {
            return AIToolResult.error("Failed to build actor path: " + e.getMessage());
        }
    }

    private int insert(KeyframeChannel<?> channel, int tick, JsonObject json, String key)
    {
        if (!json.has(key))
        {
            return 0;
        }

        @SuppressWarnings("rawtypes")
        KeyframeChannel raw = channel;
        int index = raw.insert(tick, json.get(key).getAsDouble());
        AIToolUtils.applyKeyframeMeta(channel, index, json);

        return 1;
    }

    private JsonObject cloneWithValue(JsonObject source, String key, double value)
    {
        JsonObject clone = source.deepCopy();
        clone.addProperty(key, value);

        return clone;
    }

    private float inferYaw(JsonArray points, int index)
    {
        JsonObject current = points.get(index).getAsJsonObject();
        JsonObject target = index < points.size() - 1 ? points.get(index + 1).getAsJsonObject() : current;
        double x1 = current.get("x").getAsDouble();
        double z1 = current.get("z").getAsDouble();
        double x2 = target.get("x").getAsDouble();
        double z2 = target.get("z").getAsDouble();
        double dx = x2 - x1;
        double dz = z2 - z1;

        if (Math.abs(dx) < 0.0001D && Math.abs(dz) < 0.0001D)
        {
            return current.has("yaw") ? current.get("yaw").getAsFloat() : 0F;
        }

        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }
}
