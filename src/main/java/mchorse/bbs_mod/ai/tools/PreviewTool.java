package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.film.Film;

/**
 * Triggers playback preview after AI changes.
 * This is a client-side action that will be handled by the UI layer.
 */
public class PreviewTool implements IAITool
{
    @Override
    public String getName()
    {
        return "preview_tool";
    }

    @Override
    public String getDescription()
    {
        return "Request a playback preview of the current film from a specific tick. " +
            "Use this after making changes to let the user see the result.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"from_tick\":{\"type\":\"integer\",\"description\":\"Tick to start preview from (default: 0)\"}," +
            "\"to_tick\":{\"type\":\"integer\",\"description\":\"Tick to end preview at (default: end of film)\"}" +
            "},\"required\":[]}";
    }

    @Override
    public AIToolResult execute(String argumentsJson, AIToolContext context)
    {
        Film film = context.getFilm();
        if (film == null) return AIToolResult.error("No film is currently open.");

        try
        {
            JsonObject args = JsonParser.parseString(argumentsJson).getAsJsonObject();
            int fromTick = args.has("from_tick") ? args.get("from_tick").getAsInt() : 0;
            int toTick = args.has("to_tick") ? args.get("to_tick").getAsInt() : -1;

            if (fromTick < 0) fromTick = 0;

            /* Store preview request in context for the UI layer to pick up */
            context.requestPreview(fromTick, toTick);

            String message = "Preview requested from tick " + fromTick;
            if (toTick > 0)
            {
                message += " to tick " + toTick;
            }

            return AIToolResult.success(message);
        }
        catch (Exception e)
        {
            return AIToolResult.error("Failed to request preview: " + e.getMessage());
        }
    }
}
