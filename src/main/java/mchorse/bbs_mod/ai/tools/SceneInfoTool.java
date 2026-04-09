package mchorse.bbs_mod.ai.tools;

import mchorse.bbs_mod.film.Film;

public class SceneInfoTool implements IAITool
{
    @Override
    public String getName()
    {
        return "scene_info";
    }

    @Override
    public String getDescription()
    {
        return "Get current film information: duration, FPS (20 ticks/sec), actor count, and camera clip count.";
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

        int duration = film.camera.calculateDuration();
        int actorCount = film.replays.getList().size();
        int cameraClipCount = film.camera.get().size();
        float seconds = duration / 20.0f;

        StringBuilder sb = new StringBuilder();
        sb.append("Film: ").append(context.getFilmId()).append("\n");
        sb.append("Duration: ").append(duration).append(" ticks (").append(String.format("%.1f", seconds)).append(" seconds)\n");
        sb.append("Tick rate: 20 ticks/second\n");
        sb.append("Actor count: ").append(actorCount).append("\n");
        sb.append("Camera clips: ").append(cameraClipCount);

        return AIToolResult.success(sb.toString());
    }
}
