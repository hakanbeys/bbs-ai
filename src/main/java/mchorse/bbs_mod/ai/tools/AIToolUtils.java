package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ByteType;
import mchorse.bbs_mod.data.types.DoubleType;
import mchorse.bbs_mod.data.types.IntType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.LongType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.data.types.StringType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AIToolUtils
{
    public static Replay requireReplay(Film film, int actorIndex)
    {
        if (film == null)
        {
            return null;
        }

        List<Replay> replays = film.replays.getList();

        if (actorIndex < 0 || actorIndex >= replays.size())
        {
            return null;
        }

        return replays.get(actorIndex);
    }

    public static String normalizeModelKey(String input)
    {
        if (input == null)
        {
            return "";
        }

        String value = input.trim().replace('\\', '/');

        if (value.startsWith("assets:"))
        {
            value = value.substring("assets:".length());
        }

        if (value.startsWith("models/"))
        {
            value = value.substring("models/".length());
        }

        if (value.endsWith("/config.json"))
        {
            return value.substring(0, value.length() - "/config.json".length());
        }

        int slash = value.lastIndexOf('/');

        if (slash >= 0)
        {
            return value.substring(0, slash);
        }

        return value;
    }

    public static String normalizeParticleKey(String input)
    {
        if (input == null)
        {
            return "";
        }

        String value = input.trim().replace('\\', '/');

        if (value.startsWith("assets:"))
        {
            value = value.substring("assets:".length());
        }

        if (value.startsWith("particles/"))
        {
            value = value.substring("particles/".length());
        }

        if (value.endsWith(".json"))
        {
            value = value.substring(0, value.length() - ".json".length());
        }

        return value;
    }

    public static String normalizeAssetLink(String input)
    {
        if (input == null)
        {
            return "";
        }

        String value = input.trim().replace('\\', '/');

        if (value.contains(":"))
        {
            return value;
        }

        return "assets:" + value;
    }

    public static String normalizeQuery(String query)
    {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }

    public static List<String> scanFiles(File root, String relativePrefix, String query, String... suffixes)
    {
        List<String> results = new ArrayList<>();

        if (root == null || !root.exists() || !root.isDirectory())
        {
            return results;
        }

        scanFiles(root, root, relativePrefix, normalizeQuery(query), results, suffixes);

        return results;
    }

    private static void scanFiles(File root, File folder, String relativePrefix, String query, List<String> results, String... suffixes)
    {
        File[] files = folder.listFiles();

        if (files == null)
        {
            return;
        }

        for (File file : files)
        {
            if (file.isDirectory())
            {
                scanFiles(root, file, relativePrefix, query, results, suffixes);
                continue;
            }

            String path = root.toPath().relativize(file.toPath()).toString().replace('\\', '/');
            String fullPath = relativePrefix + path;
            String lower = fullPath.toLowerCase(Locale.ROOT);

            if (!matchesSuffix(lower, suffixes))
            {
                continue;
            }

            if (!query.isEmpty() && !lower.contains(query))
            {
                continue;
            }

            results.add(fullPath);
        }
    }

    private static boolean matchesSuffix(String path, String... suffixes)
    {
        if (suffixes == null || suffixes.length == 0)
        {
            return true;
        }

        for (String suffix : suffixes)
        {
            if (path.endsWith(suffix))
            {
                return true;
            }
        }

        return false;
    }

    public static BaseValueBasic getFormProperty(Replay replay, String propertyPath)
    {
        return replay == null || replay.form.get() == null ? null : FormUtils.getProperty(replay.form.get(), propertyPath);
    }

    public static BaseType jsonToData(JsonElement element)
    {
        if (element == null || element.isJsonNull())
        {
            return null;
        }

        if (element.isJsonObject())
        {
            JsonObject json = element.getAsJsonObject();
            MapType map = new MapType();

            for (String key : json.keySet())
            {
                BaseType value = jsonToData(json.get(key));

                if (value != null)
                {
                    map.put(key, value);
                }
            }

            return map;
        }

        if (element.isJsonArray())
        {
            JsonArray json = element.getAsJsonArray();
            ListType list = new ListType();

            for (JsonElement item : json)
            {
                BaseType value = jsonToData(item);

                if (value != null)
                {
                    list.add(value);
                }
            }

            return list;
        }

        if (element.getAsJsonPrimitive().isBoolean())
        {
            return new ByteType(element.getAsBoolean());
        }

        if (element.getAsJsonPrimitive().isString())
        {
            return new StringType(element.getAsString());
        }

        double number = element.getAsDouble();

        if (Math.floor(number) == number)
        {
            long longValue = (long) number;

            if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE)
            {
                return new IntType((int) longValue);
            }

            return new LongType(longValue);
        }

        return new DoubleType(number);
    }

    public static void applyKeyframeMeta(KeyframeChannel<?> channel, int index, JsonObject json)
    {
        if (channel == null || !channel.has(index) || json == null)
        {
            return;
        }

        Keyframe<?> keyframe = channel.get(index);

        if (json.has("interpolation"))
        {
            BaseType data;
            JsonElement interpolation = json.get("interpolation");

            if (interpolation.isJsonObject())
            {
                data = jsonToData(interpolation);
            }
            else
            {
                MapType map = new MapType();
                map.putString("key", interpolation.getAsString());

                if (json.has("interpolation_args") && json.get("interpolation_args").isJsonArray())
                {
                    ListType args = new ListType();

                    for (JsonElement arg : json.getAsJsonArray("interpolation_args"))
                    {
                        args.addDouble(arg.getAsDouble());
                    }

                    map.put("args", args);
                }

                data = map;
            }

            keyframe.getInterpolation().fromData(data);
        }

        if (json.has("duration")) keyframe.setDuration(json.get("duration").getAsFloat());
        if (json.has("lx")) keyframe.lx = json.get("lx").getAsFloat();
        if (json.has("ly")) keyframe.ly = json.get("ly").getAsFloat();
        if (json.has("rx")) keyframe.rx = json.get("rx").getAsFloat();
        if (json.has("ry")) keyframe.ry = json.get("ry").getAsFloat();
    }
}
