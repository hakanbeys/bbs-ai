package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class ActorSelectTool implements IAITool
{
    @Override
    public String getName()
    {
        return "actor_select";
    }

    @Override
    public String getDescription()
    {
        return "Resolve an actor index by fuzzy name, label, or form/model text. Use this when the user refers to an actor by name instead of numeric actor_index.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"query\":{\"type\":\"string\",\"description\":\"Actor name, label, or identifying text\"}," +
            "\"limit\":{\"type\":\"integer\",\"description\":\"Maximum matches to return\"}" +
            "},\"required\":[\"query\"]}";
    }

    @Override
    public AIToolResult execute(String argumentsJson, AIToolContext context)
    {
        Film film = context.getFilm();

        if (film == null)
        {
            return AIToolResult.error("No film is currently open.");
        }

        try
        {
            JsonObject args = JsonParser.parseString(argumentsJson).getAsJsonObject();
            String query = args.get("query").getAsString().trim();
            int limit = args.has("limit") ? Math.max(1, args.get("limit").getAsInt()) : 5;

            if (query.isEmpty())
            {
                return AIToolResult.error("query is required.");
            }

            List<Replay> replays = film.replays.getList();

            if (replays.isEmpty())
            {
                return AIToolResult.success("No actors exist in the scene. Use actor_add to create one.");
            }

            String normalizedQuery = normalize(query);
            List<ActorMatch> matches = new ArrayList<>();

            for (int i = 0; i < replays.size(); i++)
            {
                Replay replay = replays.get(i);
                int score = score(replay, normalizedQuery);

                if (score > 0)
                {
                    matches.add(new ActorMatch(i, replay, score));
                }
            }

            if (matches.isEmpty())
            {
                return AIToolResult.error("No actor matched \"" + query + "\". Use actor_list to inspect available actors.");
            }

            matches.sort((a, b) -> Integer.compare(b.score, a.score));
            ActorMatch best = matches.get(0);

            StringBuilder builder = new StringBuilder();
            builder.append("Best actor match for \"").append(query).append("\": [")
                .append(best.index).append("] \"").append(best.replay.getName()).append("\"");

            Form form = best.replay.form.get();
            if (form instanceof ModelForm modelForm && !modelForm.model.get().isEmpty())
            {
                builder.append(" | model=").append(modelForm.model.get());
            }

            builder.append("\nMatches:\n");

            for (int i = 0; i < Math.min(limit, matches.size()); i++)
            {
                ActorMatch match = matches.get(i);
                builder.append("- [").append(match.index).append("] \"").append(match.replay.getName()).append("\" score=").append(match.score).append("\n");
            }

            return AIToolResult.success(builder.toString());
        }
        catch (Exception e)
        {
            return AIToolResult.error("Actor select failed: " + e.getMessage());
        }
    }

    private int score(Replay replay, String query)
    {
        String name = normalize(replay.getName());
        String label = normalize(replay.label.get());
        String model = "";
        Form form = replay.form.get();

        if (form instanceof ModelForm modelForm)
        {
            model = normalize(modelForm.model.get());
        }

        if (!label.isEmpty() && label.equals(query))
        {
            return 120;
        }
        if (name.equals(query))
        {
            return 110;
        }
        if (!model.isEmpty() && model.equals(query))
        {
            return 100;
        }

        int score = 0;

        if (!label.isEmpty() && label.contains(query))
        {
            score = Math.max(score, 90);
        }
        if (name.contains(query))
        {
            score = Math.max(score, 80);
        }
        if (!model.isEmpty() && model.contains(query))
        {
            score = Math.max(score, 70);
        }
        if (query.contains(name) && !name.isEmpty())
        {
            score = Math.max(score, 60);
        }
        if (!label.isEmpty() && query.contains(label))
        {
            score = Math.max(score, 60);
        }

        return score;
    }

    private String normalize(String value)
    {
        if (value == null)
        {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");

        return normalized.toLowerCase().trim();
    }

    private static class ActorMatch
    {
        public final int index;
        public final Replay replay;
        public final int score;

        public ActorMatch(int index, Replay replay, int score)
        {
            this.index = index;
            this.replay = replay;
            this.score = score;
        }
    }
}
