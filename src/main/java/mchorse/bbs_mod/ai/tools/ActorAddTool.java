package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.base.BaseValue;

public class ActorAddTool implements IAITool
{
    @Override
    public String getName()
    {
        return "actor_add";
    }

    @Override
    public String getDescription()
    {
        return "Add a new actor (replay) to the scene. Specify a label/name, and optionally set initial position.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"label\":{\"type\":\"string\",\"description\":\"Display name for the actor\"}," +
            "\"x\":{\"type\":\"number\",\"description\":\"Initial X position\"}," +
            "\"y\":{\"type\":\"number\",\"description\":\"Initial Y position\"}," +
            "\"z\":{\"type\":\"number\",\"description\":\"Initial Z position\"}," +
            "\"yaw\":{\"type\":\"number\",\"description\":\"Initial yaw rotation in degrees\"}," +
            "\"pitch\":{\"type\":\"number\",\"description\":\"Initial pitch rotation in degrees\"}" +
            "},\"required\":[\"label\"]}";
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
            String label = args.has("label") ? args.get("label").getAsString() : "Actor";
            double x = args.has("x") ? args.get("x").getAsDouble() : 0;
            double y = args.has("y") ? args.get("y").getAsDouble() : 64;
            double z = args.has("z") ? args.get("z").getAsDouble() : 0;
            double yaw = args.has("yaw") ? args.get("yaw").getAsDouble() : 0;
            double pitch = args.has("pitch") ? args.get("pitch").getAsDouble() : 0;

            final int[] replayIndex = {-1};

            BaseValue.edit(film, (f) ->
            {
                Replay replay = f.replays.addReplay();
                replay.label.set(label);
                replay.keyframes.x.insert(0, x);
                replay.keyframes.y.insert(0, y);
                replay.keyframes.z.insert(0, z);
                replay.keyframes.yaw.insert(0, yaw);
                replay.keyframes.pitch.insert(0, pitch);
                replay.keyframes.bodyYaw.insert(0, yaw);
                replay.keyframes.headYaw.insert(0, yaw);

                replayIndex[0] = f.replays.getList().size() - 1;
            });

            return AIToolResult.success(
                "Added actor \"" + label + "\" at position " +
                String.format("%.1f, %.1f, %.1f", x, y, z) +
                " (index: " + replayIndex[0] + ")", 1
            );
        }
        catch (Exception e)
        {
            return AIToolResult.error("Failed to add actor: " + e.getMessage());
        }
    }
}
