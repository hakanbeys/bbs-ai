package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.BBSMod;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AssetSearchTool implements IAITool
{
    @Override
    public String getName()
    {
        return "asset_search";
    }

    @Override
    public String getDescription()
    {
        return "Search exact available BBS assets before assigning them. Use this before setting a model, particle effect, texture, or audio path.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"query\":{\"type\":\"string\",\"description\":\"Substring to search for. Leave empty to list everything in selected categories.\"}," +
            "\"categories\":{\"type\":\"array\",\"description\":\"Categories to search: models, particles, textures, audio.\",\"items\":{\"type\":\"string\"}}," +
            "\"max_results\":{\"type\":\"integer\",\"description\":\"Maximum number of results per category (default 20)\"}" +
            "},\"required\":[]}";
    }

    @Override
    public AIToolResult execute(String argumentsJson, AIToolContext context)
    {
        try
        {
            JsonObject args = JsonParser.parseString(argumentsJson).getAsJsonObject();
            String query = args.has("query") ? args.get("query").getAsString() : "";
            int maxResults = args.has("max_results") ? Math.max(1, args.get("max_results").getAsInt()) : 20;
            List<String> categories = parseCategories(args);
            File assetsFolder = BBSMod.getAssetsFolder();
            StringBuilder output = new StringBuilder();
            int total = 0;

            for (String category : categories)
            {
                List<String> matches = searchCategory(assetsFolder, category, query);

                if (matches.isEmpty())
                {
                    output.append(category).append(": no matches\n");
                    continue;
                }

                output.append(category).append(" (").append(Math.min(matches.size(), maxResults)).append("/").append(matches.size()).append("):\n");

                for (int i = 0; i < matches.size() && i < maxResults; i++)
                {
                    output.append("  ").append(matches.get(i)).append("\n");
                    total += 1;
                }
            }

            if (total == 0)
            {
                return AIToolResult.success("No matching assets found. Do not invent keys or paths.");
            }

            output.append("Use only exact returned keys/paths. Do not invent missing assets.");

            return AIToolResult.success(output.toString());
        }
        catch (Exception e)
        {
            return AIToolResult.error("Failed to search assets: " + e.getMessage());
        }
    }

    private List<String> parseCategories(JsonObject args)
    {
        List<String> categories = new ArrayList<>();

        if (args.has("categories") && args.get("categories").isJsonArray())
        {
            JsonArray array = args.getAsJsonArray("categories");

            for (int i = 0; i < array.size(); i++)
            {
                String value = array.get(i).getAsString().toLowerCase(Locale.ROOT);

                if (!categories.contains(value))
                {
                    categories.add(value);
                }
            }
        }

        if (categories.isEmpty())
        {
            categories.add("models");
            categories.add("particles");
            categories.add("textures");
        }

        return categories;
    }

    private List<String> searchCategory(File assetsFolder, String category, String query)
    {
        String normalized = AIToolUtils.normalizeQuery(query);

        if ("models".equals(category))
        {
            List<String> files = AIToolUtils.scanFiles(new File(assetsFolder, "models"), "", normalized,
                ".bbs.json", ".geo.json", ".bobj", ".obj", ".vox", "config.json");
            List<String> keys = new ArrayList<>();

            for (String file : files)
            {
                String key = AIToolUtils.normalizeModelKey(file);

                if (!key.isEmpty() && !keys.contains(key))
                {
                    keys.add(key);
                }
            }

            return keys;
        }

        if ("particles".equals(category))
        {
            List<String> files = AIToolUtils.scanFiles(new File(assetsFolder, "particles"), "", normalized, ".json");
            List<String> keys = new ArrayList<>();

            for (String file : files)
            {
                String key = AIToolUtils.normalizeParticleKey(file);

                if (!key.isEmpty() && !keys.contains(key))
                {
                    keys.add(key);
                }
            }

            return keys;
        }

        if ("textures".equals(category))
        {
            List<String> files = AIToolUtils.scanFiles(new File(assetsFolder, "textures"), "textures/", normalized,
                ".png", ".jpg", ".jpeg", ".webp");
            List<String> links = new ArrayList<>();

            for (String file : files)
            {
                links.add("assets:" + file);
            }

            return links;
        }

        if ("audio".equals(category))
        {
            List<String> files = AIToolUtils.scanFiles(new File(assetsFolder, "audio"), "audio/", normalized,
                ".ogg", ".wav", ".mp3");
            List<String> links = new ArrayList<>();

            for (String file : files)
            {
                links.add("assets:" + file);
            }

            return links;
        }

        return new ArrayList<>();
    }
}
