package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.camera.clips.overwrite.KeyframeClip;
import mchorse.bbs_mod.camera.clips.overwrite.PathClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.settings.values.base.BaseValue;

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
        return "Create a cinematic camera shot as either a keyframe clip or a smooth path clip.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"mode\":{\"type\":\"string\",\"enum\":[\"keyframe\",\"path\"],\"description\":\"Shot type\"}," +
            "\"title\":{\"type\":\"string\"},\"tick\":{\"type\":\"integer\"},\"duration\":{\"type\":\"integer\"}," +
            "\"points\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{" +
            "\"tick\":{\"type\":\"integer\"},\"x\":{\"type\":\"number\"},\"y\":{\"type\":\"number\"},\"z\":{\"type\":\"number\"}," +
            "\"yaw\":{\"type\":\"number\"},\"pitch\":{\"type\":\"number\"},\"roll\":{\"type\":\"number\"},\"fov\":{\"type\":\"number\"}," +
            "\"interpolation\":{\"type\":\"string\"}" +
            "},\"required\":[\"x\",\"y\",\"z\"]}}," +
            "\"point_interpolation\":{\"type\":\"string\"},\"angle_interpolation\":{\"type\":\"string\"}" +
            "},\"required\":[\"points\"]}";
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
            String mode = args.has("mode") ? args.get("mode").getAsString() : "path";
            String title = args.has("title") ? args.get("title").getAsString() : "Shot";
            int tick = args.has("tick") ? args.get("tick").getAsInt() : 0;
            int duration = args.has("duration") ? args.get("duration").getAsInt() : 40;
            JsonArray points = args.getAsJsonArray("points");

            if (points == null || points.isEmpty())
            {
                return AIToolResult.error("points is required.");
            }

            if (mode.equals("keyframe"))
            {
                return addKeyframeShot(film, title, tick, duration, points);
            }

            return addPathShot(film, title, tick, duration, points, args);
        }
        catch (Exception e)
        {
            return AIToolResult.error("Camera shot failed: " + e.getMessage());
        }
    }

    private AIToolResult addKeyframeShot(Film film, String title, int tick, int duration, JsonArray points)
    {
        final int[] count = {0};

        BaseValue.edit(film, (f) ->
        {
            KeyframeClip clip = new KeyframeClip();
            clip.title.set(title);
            clip.tick.set(tick);
            clip.duration.set(duration);

            for (JsonElement element : points)
            {
                JsonObject point = element.getAsJsonObject();
                int relativeTick = point.has("tick") ? point.get("tick").getAsInt() : 0;

                if (point.has("x")) count[0] += insert(clip.x, relativeTick, point.get("x").getAsDouble(), point);
                if (point.has("y")) count[0] += insert(clip.y, relativeTick, point.get("y").getAsDouble(), point);
                if (point.has("z")) count[0] += insert(clip.z, relativeTick, point.get("z").getAsDouble(), point);
                if (point.has("yaw")) count[0] += insert(clip.yaw, relativeTick, point.get("yaw").getAsDouble(), point);
                if (point.has("pitch")) count[0] += insert(clip.pitch, relativeTick, point.get("pitch").getAsDouble(), point);
                if (point.has("roll")) count[0] += insert(clip.roll, relativeTick, point.get("roll").getAsDouble(), point);
                if (point.has("fov")) count[0] += insert(clip.fov, relativeTick, point.get("fov").getAsDouble(), point);
            }

            f.camera.addClip(clip);
        });

        return AIToolResult.success("Added keyframe camera shot \"" + title + "\" with " + count[0] + " keyframe values.", 1);
    }

    private AIToolResult addPathShot(Film film, String title, int tick, int duration, JsonArray points, JsonObject args)
    {
        BaseValue.edit(film, (f) ->
        {
            PathClip clip = new PathClip();
            clip.title.set(title);
            clip.tick.set(tick);
            clip.duration.set(duration);

            String pointInterp = AIToolUtils.readInterpolationName(args, "point_interpolation");
            String angleInterp = AIToolUtils.readInterpolationName(args, "angle_interpolation");

            if (pointInterp != null)
            {
                clip.interpolationPoint.setInterp(AIToolUtils.resolveInterpolation(pointInterp));
            }

            if (angleInterp != null)
            {
                clip.interpolationAngle.setInterp(AIToolUtils.resolveInterpolation(angleInterp));
            }

            for (JsonElement element : points)
            {
                JsonObject point = element.getAsJsonObject();
                float x = point.has("x") ? point.get("x").getAsFloat() : 0F;
                float y = point.has("y") ? point.get("y").getAsFloat() : 0F;
                float z = point.has("z") ? point.get("z").getAsFloat() : 0F;
                float yaw = point.has("yaw") ? point.get("yaw").getAsFloat() : 0F;
                float pitch = point.has("pitch") ? point.get("pitch").getAsFloat() : 0F;
                float roll = point.has("roll") ? point.get("roll").getAsFloat() : 0F;
                float fov = point.has("fov") ? point.get("fov").getAsFloat() : 70F;

                clip.points.add(new Position(x, y, z, yaw, pitch, roll, fov));
            }

            f.camera.addClip(clip);
        });

        return AIToolResult.success("Added path camera shot \"" + title + "\" with " + points.size() + " control points.", 1);
    }

    private int insert(mchorse.bbs_mod.utils.keyframes.KeyframeChannel<Double> channel, int tick, double value, JsonObject point)
    {
        int index = channel.insert(tick, value);

        return 1 + AIToolUtils.applyInterpolation(channel, index, point, null);
    }
}
