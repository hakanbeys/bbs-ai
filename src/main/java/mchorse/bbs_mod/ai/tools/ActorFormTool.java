package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.base.BaseValue;

public class ActorFormTool implements IAITool
{
    @Override
    public String getName()
    {
        return "actor_form";
    }

    @Override
    public String getDescription()
    {
        return "Assign or inspect an actor's form. Use this to attach a model form with an exact model key before animating form properties.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"action\":{\"type\":\"string\",\"enum\":[\"assign\",\"inspect\"],\"description\":\"assign or inspect the actor form\"}," +
            "\"actor_index\":{\"type\":\"integer\",\"description\":\"Actor index from actor_list\"}," +
            "\"model_key\":{\"type\":\"string\",\"description\":\"Exact model key, e.g. character/hero\"}," +
            "\"texture_path\":{\"type\":\"string\",\"description\":\"Optional exact texture path\"}" +
            "},\"required\":[\"actor_index\"]}";
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
            int actorIndex = args.get("actor_index").getAsInt();
            String action = args.has("action") ? args.get("action").getAsString() : "assign";

            Replay replay = AIToolUtils.getReplay(film, actorIndex);

            if (replay == null)
            {
                return AIToolResult.error(AIToolUtils.replayOutOfRangeMessage(film, actorIndex));
            }

            if (action.equals("inspect"))
            {
                Form form = replay.form.get();

                if (form == null)
                {
                    return AIToolResult.success("Actor \"" + replay.getName() + "\" has no form assigned.");
                }

                if (form instanceof ModelForm modelForm)
                {
                    return AIToolResult.success("Actor \"" + replay.getName() + "\" uses ModelForm with model=\"" + modelForm.model.get() + "\" texture=\"" + modelForm.texture.get() + "\".");
                }

                return AIToolResult.success("Actor \"" + replay.getName() + "\" uses form type " + form.getClass().getSimpleName() + ".");
            }

            String modelKey = args.has("model_key") ? args.get("model_key").getAsString() : "";
            String texturePath = args.has("texture_path") ? args.get("texture_path").getAsString() : "";

            if (modelKey.isEmpty())
            {
                return AIToolResult.error("model_key is required for assign.");
            }

            BaseValue.edit(film, (f) ->
            {
                ModelForm form = new ModelForm();
                form.animatable.set(true);
                form.model.set(modelKey);

                if (!texturePath.isEmpty())
                {
                    form.texture.set(Link.create(texturePath));
                }

                replay.form.set(form);
            });

            return AIToolResult.success("Assigned model form \"" + modelKey + "\" to actor \"" + replay.getName() + "\".", 1);
        }
        catch (Exception e)
        {
            return AIToolResult.error("Actor form tool failed: " + e.getMessage());
        }
    }
}
