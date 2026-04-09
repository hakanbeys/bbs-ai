package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import java.util.List;

/**
 * Aligns timing across multiple actors by shifting their keyframes.
 */
public class SyncTool implements IAITool
{
    @Override
    public String getName()
    {
        return "sync_tool";
    }

    @Override
    public String getDescription()
    {
        return "Align timing across multiple actors by shifting all their keyframes by a tick offset. " +
            "Useful for synchronizing entrances, making actors move together, or staggering animations.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"operations\":{\"type\":\"array\",\"description\":\"Array of sync operations\",\"items\":{\"type\":\"object\",\"properties\":{" +
            "\"actor_index\":{\"type\":\"integer\",\"description\":\"Index of the actor to shift\"}," +
            "\"tick_offset\":{\"type\":\"number\",\"description\":\"Number of ticks to shift (positive = later, negative = earlier)\"}" +
            "},\"required\":[\"actor_index\",\"tick_offset\"]}}" +
            "},\"required\":[\"operations\"]}";
    }

    @Override
    public AIToolResult execute(String argumentsJson, AIToolContext context)
    {
        Film film = context.getFilm();
        if (film == null) return AIToolResult.error("No film is currently open.");

        try
        {
            JsonObject args = JsonParser.parseString(argumentsJson).getAsJsonObject();
            JsonArray operations = args.getAsJsonArray("operations");

            if (operations == null || operations.size() == 0)
            {
                return AIToolResult.error("No operations provided.");
            }

            List<Replay> replays = film.replays.getList();
            StringBuilder result = new StringBuilder();
            final int[] totalShifted = {0};

            BaseValue.edit(film, (f) ->
            {
                for (JsonElement element : operations)
                {
                    JsonObject op = element.getAsJsonObject();
                    int actorIndex = op.get("actor_index").getAsInt();
                    float tickOffset = op.get("tick_offset").getAsFloat();

                    if (actorIndex < 0 || actorIndex >= replays.size())
                    {
                        continue;
                    }

                    if (tickOffset == 0)
                    {
                        continue;
                    }

                    Replay replay = replays.get(actorIndex);
                    replay.keyframes.shift(tickOffset);
                    totalShifted[0]++;

                    result.append("Shifted actor \"").append(replay.getName())
                        .append("\" by ").append(tickOffset > 0 ? "+" : "")
                        .append(tickOffset).append(" ticks\n");
                }
            });

            if (totalShifted[0] == 0)
            {
                return AIToolResult.error("No actors were shifted. Check actor indices.");
            }

            return AIToolResult.success(
                "Synced " + totalShifted[0] + " actor(s):\n" + result.toString(),
                totalShifted[0]
            );
        }
        catch (Exception e)
        {
            return AIToolResult.error("Failed to sync actors: " + e.getMessage());
        }
    }
}
