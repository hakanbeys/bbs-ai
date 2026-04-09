package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;

import java.util.List;

public class FormInspectTool implements IAITool
{
    @Override
    public String getName()
    {
        return "form_inspect";
    }

    @Override
    public String getDescription()
    {
        return "Inspect an actor's current form, including exact property paths that can be edited. Use this before form_property_edit.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"actor_index\":{\"type\":\"integer\",\"description\":\"Actor index from actor_list\"}," +
            "\"include_values\":{\"type\":\"boolean\",\"description\":\"Include current values (default true)\"}" +
            "},\"required\":[\"actor_index\"]}";
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
            boolean includeValues = !args.has("include_values") || args.get("include_values").getAsBoolean();
            Replay replay = AIToolUtils.requireReplay(film, actorIndex);

            if (replay == null)
            {
                return AIToolResult.error("Actor index " + actorIndex + " is out of range. Use actor_list first.");
            }

            Form form = replay.form.get();

            if (form == null)
            {
                return AIToolResult.success("Actor \"" + replay.getName() + "\" has no form assigned.");
            }

            List<String> paths = FormUtils.collectPropertyPaths(form);
            StringBuilder output = new StringBuilder();

            output.append("Actor ").append(actorIndex).append(": \"").append(replay.getName()).append("\"\n");
            output.append("Form type: ").append(form.getFormId()).append("\n");
            output.append("Display name: ").append(form.getDisplayName()).append("\n");
            output.append("Editable property paths (").append(paths.size()).append("):\n");

            for (String path : paths)
            {
                output.append("  ").append(path);

                if (includeValues)
                {
                    BaseValueBasic property = FormUtils.getProperty(form, path);

                    if (property != null)
                    {
                        output.append(" = ").append(DataToString.toString(property.toData()));
                    }
                }

                output.append("\n");
            }

            output.append("Use only exact property paths from this list.");

            return AIToolResult.success(output.toString());
        }
        catch (Exception e)
        {
            return AIToolResult.error("Failed to inspect form: " + e.getMessage());
        }
    }
}
