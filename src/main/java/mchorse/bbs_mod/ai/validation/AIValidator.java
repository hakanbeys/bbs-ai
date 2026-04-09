package mchorse.bbs_mod.ai.validation;

import mchorse.bbs_mod.ai.log.AIOperationLogger;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import java.util.List;

/**
 * Validation and cleanup layer that runs after every AI operation.
 * Scans for NaN values, overlapping keyframes, and broken references.
 */
public class AIValidator
{
    /**
     * Run all validations on the film.
     * Returns number of issues found and fixed.
     */
    public static int validate(Film film, String filmId)
    {
        if (film == null) return 0;

        int issues = 0;
        List<Replay> replays = film.replays.getList();

        for (int i = 0; i < replays.size(); i++)
        {
            Replay replay = replays.get(i);
            issues += validateKeyframes(replay, i, filmId);
        }

        return issues;
    }

    /**
     * Validate all keyframe channels on a replay.
     * Removes NaN values and fixes overlapping keyframes.
     */
    private static int validateKeyframes(Replay replay, int actorIndex, String filmId)
    {
        int issues = 0;

        for (KeyframeChannel<?> channel : replay.keyframes.getChannels())
        {
            issues += cleanNaN(channel, actorIndex, replay.getName(), filmId);
            issues += cleanOverlapping(channel, actorIndex, replay.getName(), filmId);
        }

        return issues;
    }

    /**
     * Remove keyframes with NaN values.
     */
    @SuppressWarnings("unchecked")
    private static int cleanNaN(KeyframeChannel<?> channel, int actorIndex, String actorName, String filmId)
    {
        int removed = 0;
        List<? extends Keyframe<?>> keyframes = channel.getKeyframes();

        for (int i = keyframes.size() - 1; i >= 0; i--)
        {
            Keyframe<?> kf = keyframes.get(i);
            if (kf.getValue() instanceof Number)
            {
                double val = ((Number) kf.getValue()).doubleValue();
                if (Double.isNaN(val) || Double.isInfinite(val))
                {
                    keyframes.remove(i);
                    removed++;
                }
            }
        }

        if (removed > 0)
        {
            AIOperationLogger.log(filmId, "validator",
                "actor[" + actorIndex + "] \"" + actorName + "\"",
                "Removed " + removed + " NaN/Infinite keyframe(s)");
        }

        return removed;
    }

    /**
     * Clean overlapping keyframes (same tick, same channel).
     * Keeps the last inserted one.
     */
    @SuppressWarnings("unchecked")
    private static int cleanOverlapping(KeyframeChannel<?> channel, int actorIndex, String actorName, String filmId)
    {
        int removed = 0;
        List<? extends Keyframe<?>> keyframes = channel.getKeyframes();

        for (int i = keyframes.size() - 1; i > 0; i--)
        {
            Keyframe<?> current = keyframes.get(i);
            Keyframe<?> prev = keyframes.get(i - 1);

            if (current.getTick() == prev.getTick())
            {
                /* Remove the earlier one (keep latest) */
                keyframes.remove(i - 1);
                removed++;
                i--; /* Adjust index after removal */
            }
        }

        if (removed > 0)
        {
            AIOperationLogger.log(filmId, "validator",
                "actor[" + actorIndex + "] \"" + actorName + "\"",
                "Removed " + removed + " overlapping keyframe(s)");
        }

        return removed;
    }
}
