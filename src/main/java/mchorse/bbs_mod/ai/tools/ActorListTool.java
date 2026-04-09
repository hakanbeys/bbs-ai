package mchorse.bbs_mod.ai.tools;

import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import java.util.List;

public class ActorListTool implements IAITool
{
    @Override
    public String getName()
    {
        return "actor_list";
    }

    @Override
    public String getDescription()
    {
        return "List all actors (replays) in the current film with their index, name, form, enabled status, position at tick 0, and keyframe counts.";
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

        List<Replay> replays = film.replays.getList();

        if (replays.isEmpty())
        {
            return AIToolResult.success("No actors in the scene. Use actor_add to add one.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Actors (").append(replays.size()).append("):\n");

        for (int i = 0; i < replays.size(); i++)
        {
            Replay replay = replays.get(i);
            String name = replay.getName();
            boolean enabled = replay.enabled.get();
            Form form = replay.form.get();
            String formId = form == null ? "none" : form.getFormId();
            String formName = form == null ? "" : form.getDisplayName();

            /* Count total keyframes */
            int totalKeyframes = 0;
            for (KeyframeChannel<?> channel : replay.keyframes.getChannels())
            {
                totalKeyframes += channel.getKeyframes().size();
            }

            /* Get position at tick 0 */
            double x = replay.keyframes.x.isEmpty() ? 0 : replay.keyframes.x.interpolate(0);
            double y = replay.keyframes.y.isEmpty() ? 0 : replay.keyframes.y.interpolate(0);
            double z = replay.keyframes.z.isEmpty() ? 0 : replay.keyframes.z.interpolate(0);

            sb.append("  [").append(i).append("] \"").append(name).append("\"");
            sb.append(enabled ? "" : " (DISABLED)");
            sb.append(" | pos: ").append(String.format("%.1f, %.1f, %.1f", x, y, z));
            sb.append(" | form: ").append(formId);
            if (!formName.isEmpty())
            {
                sb.append(" (").append(formName).append(")");
            }
            sb.append(" | keyframes: ").append(totalKeyframes);
            sb.append(" | looping: ").append(replay.looping.get());
            sb.append("\n");
        }

        return AIToolResult.success(sb.toString());
    }
}
