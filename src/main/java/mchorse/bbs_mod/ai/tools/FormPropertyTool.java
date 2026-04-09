package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;

public class FormPropertyTool implements IAITool
{
    @Override
    public String getName()
    {
        return "form_property_edit";
    }

    @Override
    public String getDescription()
    {
        return "Edit any current form property on an actor using exact property paths from form_inspect. Works for nested form settings and animation-capable properties.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"actor_index\":{\"type\":\"integer\",\"description\":\"Actor index from actor_list\"}," +
            "\"operations\":{\"type\":\"array\",\"description\":\"List of property edits\",\"items\":{\"type\":\"object\",\"properties\":{" +
            "\"path\":{\"type\":\"string\",\"description\":\"Exact property path from form_inspect\"}," +
            "\"value\":{\"description\":\"New value as JSON. Colors should usually be integers.\"}" +
            "},\"required\":[\"path\",\"value\"]}}" +
            "},\"required\":[\"actor_index\",\"operations\"]}";
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

            if (replay.form.get() == null)
            {
                return AIToolResult.error("Actor \"" + replay.getName() + "\" has no form assigned.");
            }

            JsonArray operations = args.getAsJsonArray("operations");

            if (operations == null || operations.size() == 0)
            {
                return AIToolResult.error("No form property operations were provided.");
            }

            final int[] changes = {0};
            StringBuilder summary = new StringBuilder();

            BaseValue.edit(film, (f) ->
            {
                for (JsonElement element : operations)
                {
                    JsonObject operation = element.getAsJsonObject();
                    String path = operation.get("path").getAsString();
                    BaseValueBasic property = AIToolUtils.getFormProperty(replay, path);

                    if (property == null)
                    {
                        summary.append("Skipped unknown path: ").append(path).append("\n");
                        continue;
                    }

                    BaseType value = AIToolUtils.jsonToData(operation.get("value"));

                    try
                    {
                        property.fromData(value);
                        changes[0] += 1;
                        summary.append("Set ").append(path).append(" = ")
                            .append(value == null ? "null" : DataToString.toString(value)).append("\n");
                    }
                    catch (Exception e)
                    {
                        summary.append("Failed ").append(path).append(": ").append(e.getMessage()).append("\n");
                    }
                }
            });

            if (changes[0] == 0)
            {
                return AIToolResult.error("No form properties were changed.\n" + summary);
            }

            return AIToolResult.success(
                "Updated " + changes[0] + " form propert" + (changes[0] == 1 ? "y" : "ies") + " on actor \"" + replay.getName() + "\"\n" + summary,
                changes[0]
            );
        }
        catch (Exception e)
        {
            return AIToolResult.error("Failed to edit form properties: " + e.getMessage());
        }
    }
}
