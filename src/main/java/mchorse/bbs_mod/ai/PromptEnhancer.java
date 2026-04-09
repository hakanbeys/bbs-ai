package mchorse.bbs_mod.ai;

import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;

import java.util.List;

/**
 * Silently rewrites user messages to be more animation-focused
 * before sending to the AI provider. Adds scene context automatically.
 */
public class PromptEnhancer
{
    /**
     * Enhance a user message with scene context.
     * The original message is preserved, but wrapped with helpful context.
     */
    public static String enhance(String userMessage, Film film)
    {
        if (film == null)
        {
            return userMessage;
        }

        StringBuilder enhanced = new StringBuilder();

        /* Add current scene state as context */
        enhanced.append("[Scene Context] ");

        List<Replay> replays = film.replays.getList();
        if (replays.isEmpty())
        {
            enhanced.append("Empty scene with no actors. ");
        }
        else
        {
            enhanced.append(replays.size()).append(" actor(s): ");
            for (int i = 0; i < replays.size(); i++)
            {
                Replay r = replays.get(i);
                if (i > 0) enhanced.append(", ");
                enhanced.append("#").append(i).append(" \"").append(r.getName()).append("\"");

                double x = r.keyframes.x.isEmpty() ? 0 : r.keyframes.x.interpolate(0);
                double y = r.keyframes.y.isEmpty() ? 0 : r.keyframes.y.interpolate(0);
                double z = r.keyframes.z.isEmpty() ? 0 : r.keyframes.z.interpolate(0);
                enhanced.append(String.format(" at (%.1f, %.1f, %.1f)", x, y, z));

                if (r.form.get() != null)
                {
                    enhanced.append(" using form \"").append(r.form.get().getDisplayName()).append("\"");
                }

                int looping = r.looping.get();
                if (looping > 0)
                {
                    enhanced.append(" loop=").append(looping);
                }

                if (!r.actions.get().isEmpty())
                {
                    enhanced.append(" actions=").append(r.actions.get().size());
                }
            }
            enhanced.append(". ");
        }

        int duration = film.camera.calculateDuration();
        int cameraClips = film.camera.get().size();
        if (duration > 0)
        {
            enhanced.append("Film duration: ").append(duration).append(" ticks (")
                .append(String.format("%.1f", duration / 20.0)).append("s). ");
        }

        enhanced.append("Camera clips: ").append(cameraClips).append(". ");
        enhanced.append(cameraClips == 0 ? "No camera timeline exists yet. " : "Camera timeline already exists. ");

        enhanced.append("\n\n[User Request] ").append(userMessage);

        return enhanced.toString();
    }
}
