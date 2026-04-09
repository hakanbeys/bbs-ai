package mchorse.bbs_mod.ai.tools;

import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.utils.clips.Clip;

import java.util.List;

public class CameraInspectTool implements IAITool
{
    @Override
    public String getName()
    {
        return "camera_inspect";
    }

    @Override
    public String getDescription()
    {
        return "Inspect the current camera timeline and list existing camera clips with type, timing, layer, and title. Use this before rebuilding or extending cinematics.";
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

        List<Clip> clips = film.camera.get();

        if (clips.isEmpty())
        {
            return AIToolResult.success("Camera timeline is empty.");
        }

        StringBuilder output = new StringBuilder();
        output.append("Camera clips (").append(clips.size()).append("):\n");

        for (int i = 0; i < clips.size(); i++)
        {
            Clip clip = clips.get(i);
            String title = clip.title.get().isEmpty() ? "-" : clip.title.get();

            output.append("  [").append(i).append("] ")
                .append(clip.getClass().getSimpleName())
                .append(" | tick ").append(clip.tick.get())
                .append(" | duration ").append(clip.duration.get())
                .append(" | layer ").append(clip.layer.get())
                .append(" | title ").append(title)
                .append("\n");
        }

        output.append("Total camera duration: ").append(film.camera.calculateDuration()).append(" ticks");

        return AIToolResult.success(output.toString());
    }
}
