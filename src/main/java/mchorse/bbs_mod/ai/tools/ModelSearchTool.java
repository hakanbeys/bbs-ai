package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.BBSMod;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ModelSearchTool implements IAITool
{
    @Override
    public String getName()
    {
        return "model_search";
    }

    @Override
    public String getDescription()
    {
        return "Search for exact available model keys. Returns ModelForm-compatible keys, not guessed filenames. Use these exact keys with actor_form.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\",\"description\":\"Search query to filter models by name (substring match). Leave empty to list all.\"}},\"required\":[]}";
    }

    @Override
    public AIToolResult execute(String argumentsJson, AIToolContext context)
    {
        String query = "";

        try
        {
            JsonObject args = JsonParser.parseString(argumentsJson).getAsJsonObject();
            if (args.has("query"))
            {
                query = args.get("query").getAsString().toLowerCase();
            }
        }
        catch (Exception e)
        {
            /* Default empty query */
        }

        File modelsDir = new File(BBSMod.getAssetsFolder(), "models");
        List<String> modelFiles = new ArrayList<>();
        List<String> models = new ArrayList<>();

        if (modelsDir.exists() && modelsDir.isDirectory())
        {
            scanModels(modelsDir, modelsDir, query, modelFiles);
        }

        for (String file : modelFiles)
        {
            String key = AIToolUtils.normalizeModelKey(file);

            if (!key.isEmpty() && !models.contains(key))
            {
                models.add(key);
            }
        }

        if (models.isEmpty())
        {
            return AIToolResult.success("No models found" + (query.isEmpty() ? ". The models folder is empty." : " matching '" + query + "'."));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(models.size()).append(" model key(s):\n");
        for (String model : models)
        {
            sb.append("  ").append(model).append("\n");
        }

        sb.append("Use these exact model keys with actor_form.");

        return AIToolResult.success(sb.toString());
    }

    private void scanModels(File root, File dir, String query, List<String> results)
    {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files)
        {
            if (file.isDirectory())
            {
                scanModels(root, file, query, results);
            }
            else
            {
                String name = file.getName().toLowerCase();
                String relative = root.toPath().relativize(file.toPath()).toString().replace('\\', '/');
                String lowerPath = relative.toLowerCase();
                boolean isModel = name.endsWith(".json") || name.endsWith(".bbmodel") ||
                    name.endsWith(".bobj") || name.endsWith(".vox") || name.endsWith(".obj");

                if (isModel && (query.isEmpty() || lowerPath.contains(query)))
                {
                    results.add("models/" + relative);
                }
            }
        }
    }
}
