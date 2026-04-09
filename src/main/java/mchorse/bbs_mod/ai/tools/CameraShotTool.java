package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.camera.clips.overwrite.IdleClip;
import mchorse.bbs_mod.camera.clips.overwrite.KeyframeClip;
import mchorse.bbs_mod.camera.clips.overwrite.PathClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

public class CameraShotTool implements IAITool
{
    @Override
    public String getName()
    {
        return "camera_shot";
    }

    @Override
    public String getDescription()
    {
        return "Create cinematic camera clips. Use path for smooth multi-point movement, keyframe for precise curves, and idle for a static shot.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"shot_type\":{\"type\":\"string\",\"description\":\"Shot type: idle, path, keyframe\"}," +
            "\"tick\":{\"type\":\"integer\",\"description\":\"Start tick for the camera clip\"}," +
            "\"duration\":{\"type\":\"integer\",\"description\":\"Clip duration in ticks\"}," +
            "\"title\":{\"type\":\"string\",\"description\":\"Optional clip title\"}," +
            "\"layer\":{\"type\":\"integer\",\"description\":\"Optional camera layer\"}," +
            "\"points\":{\"type\":\"array\",\"description\":\"Positions for idle/path shots or keyframe data for keyframe shots\",\"items\":{\"type\":\"object\",\"properties\":{" +
            "\"tick\":{\"type\":\"integer\",\"description\":\"Relative tick for keyframe shot points\"}," +
            "\"x\":{\"type\":\"number\"},\"y\":{\"type\":\"number\"},\"z\":{\"type\":\"number\"}," +
            "\"yaw\":{\"type\":\"number\"},\"pitch\":{\"type\":\"number\"},\"roll\":{\"type\":\"number\"},\"fov\":{\"type\":\"number\"}," +
            "\"distance\":{\"type\":\"number\"}," +
            "\"interpolation\":{\"type\":\"string\",\"description\":\"Optional interpolation key for keyframe shots\"}," +
            "\"interpolation_args\":{\"type\":\"array\",\"items\":{\"type\":\"number\"}}" +
            "},\"required\":[\"x\",\"y\",\"z\",\"yaw\",\"pitch\"]}}" +
            "},\"required\":[\"shot_type\",\"tick\",\"duration\"]}";
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
            String shotType = args.get("shot_type").getAsString();
            int tick = args.get("tick").getAsInt();
            int duration = Math.max(1, args.get("duration").getAsInt());
            Clip clip = createClip(args, shotType);

            if (clip == null)
            {
                return AIToolResult.error("Failed to create camera shot. Check shot_type and points.");
            }

            clip.tick.set(Math.max(0, tick));
            clip.duration.set(duration);

            if (args.has("title"))
            {
                clip.title.set(args.get("title").getAsString());
            }

            if (args.has("layer"))
            {
                clip.layer.set(Math.max(0, args.get("layer").getAsInt()));
            }

            BaseValue.edit(film, (f) ->
            {
                film.camera.addClip(clip);

                if (!args.has("layer"))
                {
                    clip.layer.set(film.camera.findFreeLayer(clip));
                }
            });

            return AIToolResult.success("Added camera " + shotType + " clip at tick " + clip.tick.get() + " for " + clip.duration.get() + " ticks", 1);
        }
        catch (Exception e)
        {
            return AIToolResult.error("Failed to create camera shot: " + e.getMessage());
        }
    }

    private Clip createClip(JsonObject args, String shotType)
    {
        JsonArray points = args.has("points") && args.get("points").isJsonArray() ? args.getAsJsonArray("points") : null;

        if ("idle".equalsIgnoreCase(shotType))
        {
            if (points == null || points.size() == 0)
            {
                return null;
            }

            IdleClip clip = new IdleClip();
            clip.position.set(parsePosition(points.get(0).getAsJsonObject()));

            return clip;
        }

        if ("path".equalsIgnoreCase(shotType))
        {
            if (points == null || points.size() < 2)
            {
                return null;
            }

            PathClip clip = new PathClip();

            for (JsonElement element : points)
            {
                clip.points.add(parsePosition(element.getAsJsonObject()));
            }

            return clip;
        }

        if ("keyframe".equalsIgnoreCase(shotType))
        {
            if (points == null || points.size() == 0)
            {
                return null;
            }

            KeyframeClip clip = new KeyframeClip();

            for (JsonElement element : points)
            {
                JsonObject point = element.getAsJsonObject();
                int relativeTick = point.has("tick") ? point.get("tick").getAsInt() : 0;

                insert(clip.x, relativeTick, point, "x");
                insert(clip.y, relativeTick, point, "y");
                insert(clip.z, relativeTick, point, "z");
                insert(clip.yaw, relativeTick, point, "yaw");
                insert(clip.pitch, relativeTick, point, "pitch");
                insert(clip.roll, relativeTick, point, "roll");
                insert(clip.fov, relativeTick, point, "fov");
                insert(clip.distance, relativeTick, point, "distance");
            }

            return clip;
        }

        return null;
    }

    private void insert(KeyframeChannel<Double> channel, int tick, JsonObject json, String key)
    {
        if (!json.has(key))
        {
            return;
        }

        int index = channel.insert(tick, json.get(key).getAsDouble());
        AIToolUtils.applyKeyframeMeta(channel, index, json);
    }

    private Position parsePosition(JsonObject json)
    {
        float x = json.has("x") ? json.get("x").getAsFloat() : 0F;
        float y = json.has("y") ? json.get("y").getAsFloat() : 0F;
        float z = json.has("z") ? json.get("z").getAsFloat() : 0F;
        float yaw = json.has("yaw") ? json.get("yaw").getAsFloat() : 0F;
        float pitch = json.has("pitch") ? json.get("pitch").getAsFloat() : 0F;
        float roll = json.has("roll") ? json.get("roll").getAsFloat() : 0F;
        float fov = json.has("fov") ? json.get("fov").getAsFloat() : 70F;

        return new Position(x, y, z, yaw, pitch, roll, fov);
    }
}
