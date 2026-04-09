package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
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
        return "Read or directly edit a form property using an exact property path discovered with form_inspect.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"action\":{\"type\":\"string\",\"enum\":[\"get\",\"set\"],\"description\":\"Read or edit a property\"}," +
            "\"actor_index\":{\"type\":\"integer\",\"description\":\"Actor index\"}," +
            "\"property_path\":{\"type\":\"string\",\"description\":\"Exact property path\"}," +
            "\"value\":{\"description\":\"New value for set action\"}" +
            "},\"required\":[\"actor_index\",\"property_path\"]}";
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
            String path = args.get("property_path").getAsString();
            String action = args.has("action") ? args.get("action").getAsString() : "set";

            Replay replay = AIToolUtils.getReplay(film, actorIndex);

            if (replay == null)
            {
                return AIToolResult.error(AIToolUtils.replayOutOfRangeMessage(film, actorIndex));
            }

            Form form = replay.form.get();

            if (form == null)
            {
                return AIToolResult.error("Actor \"" + replay.getName() + "\" has no form assigned.");
            }

            BaseValueBasic property = FormUtils.getProperty(form, path);

            if (property == null)
            {
                return AIToolResult.error("Property not found: " + path);
            }

            if (action.equals("get"))
            {
                return AIToolResult.success(path + " = " + property.get());
            }

            if (!args.has("value"))
            {
                return AIToolResult.error("value is required for set.");
            }

            Object parsed = AIToolUtils.parsePropertyValue(property, args.get("value"));

            BaseValue.edit(film, (f) -> property.set(parsed));

            return AIToolResult.success("Set " + path + " = " + property.get() + " on \"" + replay.getName() + "\".", 1);
        }
        catch (Exception e)
        {
            return AIToolResult.error("Form property edit failed: " + e.getMessage());
        }
    }
}
