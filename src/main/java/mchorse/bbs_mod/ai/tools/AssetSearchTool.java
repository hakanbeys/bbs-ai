package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.BBSMod;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        return "Search exact BBS assets that already exist in the local project. Use it before assigning models, textures, particles, or audio.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"type\":{\"type\":\"string\",\"enum\":[\"all\",\"model\",\"texture\",\"particle\",\"audio\"],\"description\":\"Asset type to search\"}," +
            "\"query\":{\"type\":\"string\",\"description\":\"Substring to search for\"}," +
            "\"limit\":{\"type\":\"integer\",\"description\":\"Maximum results to return\"}" +
            "},\"required\":[]}";
    }

    @Override
    public AIToolResult execute(String argumentsJson, AIToolContext context)
    {
        try
        {
            JsonObject args = JsonParser.parseString(argumentsJson).getAsJsonObject();
            String type = args.has("type") ? args.get("type").getAsString() : "all";
            String query = args.has("query") ? args.get("query").getAsString().toLowerCase() : "";
            int limit = args.has("limit") ? Math.max(1, args.get("limit").getAsInt()) : 30;

            File assets = BBSMod.getAssetsFolder();
            List<String> results = new ArrayList<>();

            if (type.equals("all") || type.equals("model"))
            {
                results.addAll(searchModels(new File(assets, "models"), query, limit));
            }
            if ((type.equals("all") || type.equals("texture")) && results.size() < limit)
            {
                searchFiles(new File(assets, "textures"), "texture", query, limit, results, ".png", ".jpg", ".jpeg", ".webp");
            }
            if ((type.equals("all") || type.equals("particle")) && results.size() < limit)
            {
                searchFiles(new File(assets, "particles"), "particle", query, limit, results, ".json");
            }
            if ((type.equals("all") || type.equals("audio")) && results.size() < limit)
            {
                searchFiles(new File(assets, "audio"), "audio", query, limit, results, ".ogg", ".wav", ".mp3");
            }

            if (results.isEmpty())
            {
                return AIToolResult.success("No assets found for type \"" + type + "\"" + (query.isEmpty() ? "." : " matching \"" + query + "\"."));
            }

            return AIToolResult.success(AIToolUtils.listToLines("Assets", results));
        }
        catch (Exception e)
        {
            return AIToolResult.error("Asset search failed: " + e.getMessage());
        }
    }

    private List<String> searchModels(File modelsDir, String query, int limit)
    {
        Set<String> keys = new LinkedHashSet<>();
        List<String> paths = new ArrayList<>();

        AIToolUtils.scanFiles(modelsDir, modelsDir, query, limit * 3, paths, (file) ->
        {
            String name = file.getName().toLowerCase();

            return name.endsWith(".bbs.json") || name.endsWith(".geo.json") || name.endsWith(".bbmodel") || name.endsWith(".bobj") || name.endsWith(".vox") || name.endsWith(".obj");
        });

        for (String relative : paths)
        {
            int slash = relative.lastIndexOf('/');
            String key = slash >= 0 ? relative.substring(0, slash) : relative;

            if (!key.isEmpty())
            {
                keys.add("model:" + key);
            }

            if (keys.size() >= limit)
            {
                break;
            }
        }

        return new ArrayList<>(keys);
    }

    private void searchFiles(File dir, String prefix, String query, int limit, List<String> results, String... extensions)
    {
        List<String> found = new ArrayList<>();

        AIToolUtils.scanFiles(dir, dir, query, limit - results.size(), found, (file) ->
        {
            String name = file.getName().toLowerCase();

            for (String extension : extensions)
            {
                if (name.endsWith(extension))
                {
                    return true;
                }
            }

            return false;
        });

        for (String relative : found)
        {
            results.add(prefix + ":" + relative);
        }
    }
}
