package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.settings.values.base.BaseValue;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        return "Search for exact BBS model keys, inspect a model folder, or assign a model form to an actor.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"action\":{\"type\":\"string\",\"enum\":[\"search\",\"detail\",\"assign\"],\"description\":\"Action to perform\"}," +
            "\"query\":{\"type\":\"string\",\"description\":\"Substring search for model keys\"}," +
            "\"model_key\":{\"type\":\"string\",\"description\":\"Exact model key such as characters/hero\"}," +
            "\"actor_index\":{\"type\":\"integer\",\"description\":\"Actor index for assign action\"}" +
            "},\"required\":[]}";
    }

    @Override
    public AIToolResult execute(String argumentsJson, AIToolContext context)
    {
        try
        {
            JsonObject args = JsonParser.parseString(argumentsJson).getAsJsonObject();
            String action = args.has("action") ? args.get("action").getAsString() : "search";

            if (action.equals("detail"))
            {
                return detailModel(args);
            }
            if (action.equals("assign"))
            {
                return assignModel(args, context);
            }

            return searchModels(args);
        }
        catch (Exception e)
        {
            return AIToolResult.error("Model search failed: " + e.getMessage());
        }
    }

    private AIToolResult searchModels(JsonObject args)
    {
        String query = args.has("query") ? args.get("query").getAsString().toLowerCase() : "";
        List<String> keys = collectModelKeys(query, 50);

        if (keys.isEmpty())
        {
            return AIToolResult.success("No models found" + (query.isEmpty() ? "." : " matching \"" + query + "\"."));
        }

        return AIToolResult.success(AIToolUtils.listToLines("Model keys", keys));
    }

    private AIToolResult detailModel(JsonObject args)
    {
        if (!args.has("model_key"))
        {
            return AIToolResult.error("model_key is required for detail.");
        }

        String modelKey = args.get("model_key").getAsString();
        File modelDir = new File(new File(BBSMod.getAssetsFolder(), "models"), modelKey);
        StringBuilder builder = new StringBuilder();

        builder.append("Model key: ").append(modelKey).append("\n");

        if (modelDir.exists() && modelDir.isDirectory())
        {
            File[] files = modelDir.listFiles();

            builder.append("Folder: ").append(modelDir.getAbsolutePath()).append("\n");
            builder.append("Files: ").append(files == null ? 0 : files.length).append("\n");
        }
        else
        {
            builder.append("Model folder not found on disk.\n");
        }

        builder.append("Use actor_form or model_search assign with this exact model_key.");

        return AIToolResult.success(builder.toString());
    }

    private AIToolResult assignModel(JsonObject args, AIToolContext context)
    {
        Film film = context.getFilm();

        if (film == null)
        {
            return AIToolResult.error("No film is currently open.");
        }
        if (!args.has("actor_index"))
        {
            return AIToolResult.error("actor_index is required for assign.");
        }
        if (!args.has("model_key"))
        {
            return AIToolResult.error("model_key is required for assign.");
        }

        int actorIndex = args.get("actor_index").getAsInt();
        String modelKey = args.get("model_key").getAsString();
        Replay replay = AIToolUtils.getReplay(film, actorIndex);

        if (replay == null)
        {
            return AIToolResult.error(AIToolUtils.replayOutOfRangeMessage(film, actorIndex));
        }

        BaseValue.edit(film, (f) ->
        {
            ModelForm modelForm = new ModelForm();
            modelForm.animatable.set(true);
            modelForm.model.set(modelKey);
            replay.form.set(modelForm);
        });

        Form assignedForm = replay.form.get();
        List<String> props = assignedForm != null ? FormUtils.collectPropertyPaths(assignedForm) : List.of();

        return AIToolResult.success(
            "Assigned model \"" + modelKey + "\" to actor \"" + replay.getName() + "\". Animatable properties: " + props.size(),
            1
        );
    }

    private List<String> collectModelKeys(String query, int limit)
    {
        File modelsDir = new File(BBSMod.getAssetsFolder(), "models");
        List<String> matches = new ArrayList<>();

        AIToolUtils.scanFiles(modelsDir, modelsDir, query, limit * 3, matches, (file) ->
        {
            String name = file.getName().toLowerCase();

            return name.endsWith(".bbs.json") || name.endsWith(".geo.json") || name.endsWith(".bbmodel") || name.endsWith(".bobj") || name.endsWith(".vox") || name.endsWith(".obj");
        });

        Set<String> keys = new LinkedHashSet<>();

        for (String match : matches)
        {
            int slash = match.lastIndexOf('/');

            if (slash > 0)
            {
                keys.add(match.substring(0, slash));
            }

            if (keys.size() >= limit)
            {
                break;
            }
        }

        return new ArrayList<>(keys);
    }
}
