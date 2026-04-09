package mchorse.bbs_mod.ai.tools;

import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.utils.clips.Clip;

import java.util.ArrayList;
import java.util.List;

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
        return "Remove all camera clips from the current film so the AI can rebuild the cinematic timeline from scratch.";
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

        List<Clip> clips = new ArrayList<>(film.camera.get());

        if (clips.isEmpty())
        {
            return AIToolResult.success("Camera timeline is already empty.");
        }

        BaseValue.edit(film, (f) ->
        {
            for (Clip clip : clips)
            {
                film.camera.remove(clip);
            }
        });

        return AIToolResult.success("Removed " + clips.size() + " camera clip(s).", clips.size());
    }
}
