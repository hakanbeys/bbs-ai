package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import java.io.File;
import java.util.List;
import java.util.Map;

public class AIToolUtils
{
    public static Replay getReplay(Film film, int actorIndex)
    {
        if (film == null)
        {
            return null;
        }

        List<Replay> replays = film.replays.getList();

        return actorIndex >= 0 && actorIndex < replays.size() ? replays.get(actorIndex) : null;
    }

    public static String replayOutOfRangeMessage(Film film, int actorIndex)
    {
        int max = film == null ? -1 : film.replays.getList().size() - 1;

        return "Actor index " + actorIndex + " is out of range. Max: " + max;
    }

    public static int applyInterpolation(KeyframeChannel<?> channel, int index, JsonObject source, String specificField)
    {
        if (channel == null || !channel.has(index))
        {
            return 0;
        }

        String name = readInterpolationName(source, specificField);

        if (name == null || name.isEmpty())
        {
            return 0;
        }

        Keyframe<?> keyframe = channel.get(index);
        IInterp interp = resolveInterpolation(name);

        keyframe.getInterpolation().setInterp(interp);

        return 1;
    }

    public static int applyInterpolation(KeyframeChannel<?> channel, int index, String name)
    {
        if (channel == null || !channel.has(index) || name == null || name.isEmpty())
        {
            return 0;
        }

        channel.get(index).getInterpolation().setInterp(resolveInterpolation(name));

        return 1;
    }

    public static String readInterpolationName(JsonObject source, String specificField)
    {
        if (source == null)
        {
            return null;
        }

        if (specificField != null && source.has(specificField))
        {
            return source.get(specificField).getAsString();
        }

        if (source.has("interpolation"))
        {
            return source.get("interpolation").getAsString();
        }

        if (source.has("interp"))
        {
            return source.get("interp").getAsString();
        }

        if (source.has("easing"))
        {
            return source.get("easing").getAsString();
        }

        return null;
    }

    public static IInterp resolveInterpolation(String name)
    {
        if (name == null)
        {
            return Interpolations.LINEAR;
        }

        String key = name.trim().toLowerCase();

        if (key.equals("constant"))
        {
            key = "const";
        }

        IInterp interp = Interpolations.MAP.get(key);

        return interp == null ? Interpolations.LINEAR : interp;
    }

    public static Object parsePropertyValue(BaseValueBasic property, JsonElement value)
    {
        Object current = property.get();

        if (current instanceof Boolean)
        {
            return value.getAsBoolean();
        }
        if (current instanceof Integer)
        {
            return value.getAsInt();
        }
        if (current instanceof Float)
        {
            return value.getAsFloat();
        }
        if (current instanceof Double)
        {
            return value.getAsDouble();
        }
        if (current instanceof Long)
        {
            return value.getAsLong();
        }
        if (current instanceof String)
        {
            return value.getAsString();
        }

        return current;
    }

    public static void scanFiles(File root, File dir, String query, int limit, List<String> results, FileMatch match)
    {
        if (dir == null || !dir.exists() || limit <= 0)
        {
            return;
        }

        File[] files = dir.listFiles();

        if (files == null)
        {
            return;
        }

        for (File file : files)
        {
            if (results.size() >= limit)
            {
                return;
            }

            if (file.isDirectory())
            {
                scanFiles(root, file, query, limit, results, match);
            }
            else if (match.accept(file))
            {
                String relative = root.toPath().relativize(file.toPath()).toString().replace('\\', '/');

                if (query.isEmpty() || relative.toLowerCase().contains(query))
                {
                    results.add(relative);
                }
            }
        }
    }

    public static String listToLines(String title, List<String> values)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(title).append(" (").append(values.size()).append("):\n");

        for (String value : values)
        {
            builder.append("- ").append(value).append("\n");
        }

        return builder.toString();
    }

    public static interface FileMatch
    {
        boolean accept(File file);
    }
}
