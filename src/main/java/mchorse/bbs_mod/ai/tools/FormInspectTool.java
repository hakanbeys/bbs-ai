package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
        return "List exact animatable form property paths and current values for an actor.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"actor_index\":{\"type\":\"integer\",\"description\":\"Actor index from actor_list\"}" +
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

            List<String> paths = FormUtils.collectPropertyPaths(form);

            if (paths.isEmpty())
            {
                return AIToolResult.success("Form has no animatable properties.");
            }

            StringBuilder builder = new StringBuilder();
            builder.append("Form properties for \"").append(replay.getName()).append("\" (").append(paths.size()).append("):\n");

            for (String path : paths)
            {
                BaseValueBasic property = FormUtils.getProperty(form, path);
                builder.append("- ").append(path).append(" = ");
                builder.append(property == null ? "?" : property.get());
                builder.append("\n");
            }

            return AIToolResult.success(builder.toString());
        }
        catch (Exception e)
        {
            return AIToolResult.error("Form inspect failed: " + e.getMessage());
        }
    }
}
