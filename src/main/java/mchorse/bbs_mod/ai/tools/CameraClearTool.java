package mchorse.bbs_mod.ai.tools;

import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.settings.values.base.BaseValue;

public class CameraClearTool implements IAITool
{
    @Override
    public String getName()
    {
        return "camera_clear";
    }

    @Override
    public String getDescription()
    {
        return "Remove all camera clips from the current film.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{},\"required\":[]}";
    }

    @Override
    public AIToolResult execute(String argumentsJson, AIToolContext context)
    {
        Film film = context.getFilm();

        if (film == null)
        {
            return AIToolResult.error("No film is currently open.");
        }

        int count = film.camera.get().size();

        if (count == 0)
        {
            return AIToolResult.success("No camera clips to clear.");
        }

        BaseValue.edit(film, (f) ->
        {
            while (!f.camera.get().isEmpty())
            {
                f.camera.remove(f.camera.get().get(0));
            }
        });

        return AIToolResult.success("Removed " + count + " camera clip(s).", count);
    }
}
