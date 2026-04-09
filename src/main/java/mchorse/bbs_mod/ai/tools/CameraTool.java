package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.camera.clips.overwrite.KeyframeClip;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.utils.clips.Clip;

import java.util.List;

/**
 * AI tool for creating and managing camera clips.
 * Supports add, list, and remove actions for KeyframeClip camera clips.
 */
public class CameraTool implements IAITool
{
    @Override
    public String getName()
    {
        return "camera_tool";
    }

    @Override
    public String getDescription()
    {
        return "Create and manage camera clips in the film. Actions: " +
            "'add' creates a KeyframeClip camera with position/rotation keyframes at specific ticks, " +
            "'list' shows all current camera clips, " +
            "'clear' removes all camera clips. " +
            "Camera keyframe channels: x, y, z (position), yaw, pitch, roll (rotation), fov (field of view). " +
            "Tick values inside a clip are RELATIVE to the clip's start tick. " +
            "Use multiple clips for different camera angles/shots in a scene.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"action\":{\"type\":\"string\",\"enum\":[\"add\",\"list\",\"clear\"],\"description\":\"Action to perform\"}," +
            "\"title\":{\"type\":\"string\",\"description\":\"Name for the camera clip (for 'add')\"}," +
            "\"tick\":{\"type\":\"integer\",\"description\":\"Start tick of the camera clip (for 'add')\"}," +
            "\"duration\":{\"type\":\"integer\",\"description\":\"Duration in ticks (for 'add')\"}," +
            "\"keyframes\":{\"type\":\"array\",\"description\":\"Camera keyframes within the clip (for 'add'). Ticks are RELATIVE to clip start.\",\"items\":{\"type\":\"object\",\"properties\":{" +
            "\"tick\":{\"type\":\"integer\",\"description\":\"Tick relative to clip start\"}," +
            "\"x\":{\"type\":\"number\"},\"y\":{\"type\":\"number\"},\"z\":{\"type\":\"number\"}," +
            "\"yaw\":{\"type\":\"number\"},\"pitch\":{\"type\":\"number\"},\"roll\":{\"type\":\"number\"}," +
            "\"fov\":{\"type\":\"number\",\"description\":\"Field of view in degrees\"}" +
            "}}}" +
            "},\"required\":[\"action\"]}";
    }

    @Override
    public AIToolResult execute(String argumentsJson, AIToolContext context)
    {
        Film film = context.getFilm();
        if (film == null) return AIToolResult.error("No film is currently open.");

        try
        {
            JsonObject args = JsonParser.parseString(argumentsJson).getAsJsonObject();
            String action = args.get("action").getAsString();

            switch (action)
            {
                case "add": return addCamera(film, args);
                case "list": return listCameras(film);
                case "clear": return clearCameras(film);
                default: return AIToolResult.error("Unknown action: " + action);
            }
        }
        catch (Exception e)
        {
            return AIToolResult.error("Camera tool failed: " + e.getMessage());
        }
    }

    private AIToolResult addCamera(Film film, JsonObject args)
    {
        int startTick = args.has("tick") ? args.get("tick").getAsInt() : 0;
        int duration = args.has("duration") ? args.get("duration").getAsInt() : 40;
        String title = args.has("title") ? args.get("title").getAsString() : "Camera";

        JsonArray keyframes = args.has("keyframes") ? args.getAsJsonArray("keyframes") : null;
        if (keyframes == null || keyframes.isEmpty())
        {
            return AIToolResult.error("At least one keyframe is required in the 'keyframes' array.");
        }

        final int[] kfCount = {0};

        BaseValue.edit(film, (f) ->
        {
            KeyframeClip clip = new KeyframeClip();
            clip.title.set(title);
            clip.tick.set(startTick);
            clip.duration.set(duration);

            for (JsonElement kfEl : keyframes)
            {
                JsonObject kf = kfEl.getAsJsonObject();
                int t = kf.has("tick") ? kf.get("tick").getAsInt() : 0;

                if (kf.has("x")) { clip.x.insert(t, kf.get("x").getAsDouble()); kfCount[0]++; }
                if (kf.has("y")) { clip.y.insert(t, kf.get("y").getAsDouble()); kfCount[0]++; }
                if (kf.has("z")) { clip.z.insert(t, kf.get("z").getAsDouble()); kfCount[0]++; }
                if (kf.has("yaw")) { clip.yaw.insert(t, kf.get("yaw").getAsDouble()); kfCount[0]++; }
                if (kf.has("pitch")) { clip.pitch.insert(t, kf.get("pitch").getAsDouble()); kfCount[0]++; }
                if (kf.has("roll")) { clip.roll.insert(t, kf.get("roll").getAsDouble()); kfCount[0]++; }
                if (kf.has("fov")) { clip.fov.insert(t, kf.get("fov").getAsDouble()); kfCount[0]++; }
            }

            f.camera.addClip(clip);
        });

        return AIToolResult.success(
            "Added camera clip \"" + title + "\" at tick " + startTick + " (duration: " + duration +
            " ticks, " + kfCount[0] + " keyframe values).",
            1
        );
    }

    private AIToolResult listCameras(Film film)
    {
        List<Clip> clips = film.camera.get();

        if (clips.isEmpty())
        {
            return AIToolResult.success("No camera clips in the film. Use 'add' action to create one.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Camera clips (").append(clips.size()).append("):\n");

        for (int i = 0; i < clips.size(); i++)
        {
            Clip clip = clips.get(i);
            sb.append("  [").append(i).append("] ");
            sb.append("\"").append(clip.title.get().isEmpty() ? clip.getClass().getSimpleName() : clip.title.get()).append("\"");
            sb.append(" | tick: ").append(clip.tick.get());
            sb.append(" | duration: ").append(clip.duration.get());
            sb.append(" | layer: ").append(clip.layer.get());
            sb.append(" | type: ").append(clip.getClass().getSimpleName());
            sb.append("\n");
        }

        return AIToolResult.success(sb.toString());
    }

    private AIToolResult clearCameras(Film film)
    {
        List<Clip> clips = film.camera.get();
        int count = clips.size();

        if (count == 0)
        {
            return AIToolResult.success("No camera clips to remove.");
        }

        BaseValue.edit(film, (f) ->
        {
            /* Remove all clips one by one (can't modify unmodifiable list directly) */
            while (!f.camera.get().isEmpty())
            {
                f.camera.remove(f.camera.get().get(0));
            }
        });

        return AIToolResult.success("Removed " + count + " camera clip(s).", count);
    }
}
