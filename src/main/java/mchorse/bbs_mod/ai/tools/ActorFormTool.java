package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.forms.BillboardForm;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.LabelForm;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.forms.ParticleForm;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.utils.colors.Color;

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
        return "Assign or replace an actor's form. Use exact keys from asset_search/model_search before setting model, particle, or texture assets.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"actor_index\":{\"type\":\"integer\",\"description\":\"Actor index from actor_list\"}," +
            "\"form_type\":{\"type\":\"string\",\"description\":\"Form type: model, particle, label, billboard, mob\"}," +
            "\"model_key\":{\"type\":\"string\",\"description\":\"Exact model key from asset_search/models\"}," +
            "\"particle_key\":{\"type\":\"string\",\"description\":\"Exact particle effect key from asset_search/particles\"}," +
            "\"text\":{\"type\":\"string\",\"description\":\"Label text for label form\"}," +
            "\"texture\":{\"type\":\"string\",\"description\":\"Exact texture link, usually assets:textures/...\"}," +
            "\"mob_id\":{\"type\":\"string\",\"description\":\"Minecraft entity id for mob form\"}," +
            "\"display_name\":{\"type\":\"string\",\"description\":\"Optional custom form display name\"}," +
            "\"color_rgb\":{\"type\":\"integer\",\"description\":\"Optional RGB or ARGB integer color for model/label/billboard forms\"}," +
            "\"billboard\":{\"type\":\"boolean\",\"description\":\"Optional billboard toggle for label/billboard forms\"}" +
            "},\"required\":[\"actor_index\",\"form_type\"]}";
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
            Replay replay = AIToolUtils.requireReplay(film, actorIndex);

            if (replay == null)
            {
                return AIToolResult.error("Actor index " + actorIndex + " is out of range. Use actor_list first.");
            }

            String formType = args.get("form_type").getAsString();
            Form form = createForm(args, formType);

            if (form == null)
            {
                return AIToolResult.error("Unsupported or incomplete form setup for type: " + formType);
            }

            BaseValue.edit(film, (f) -> replay.form.set(form));

            return AIToolResult.success("Assigned " + form.getFormId() + " to actor \"" + replay.getName() + "\"", 1);
        }
        catch (Exception e)
        {
            return AIToolResult.error("Failed to assign actor form: " + e.getMessage());
        }
    }

    private Form createForm(JsonObject args, String formType)
    {
        if ("model".equalsIgnoreCase(formType))
        {
            if (!args.has("model_key"))
            {
                return null;
            }

            ModelForm form = new ModelForm();
            form.model.set(AIToolUtils.normalizeModelKey(args.get("model_key").getAsString()));

            if (args.has("texture"))
            {
                form.texture.set(Link.create(AIToolUtils.normalizeAssetLink(args.get("texture").getAsString())));
            }

            if (args.has("display_name"))
            {
                form.name.set(args.get("display_name").getAsString());
            }

            if (args.has("color_rgb"))
            {
                form.color.set(Color.rgba(args.get("color_rgb").getAsInt()));
            }

            return form;
        }

        if ("particle".equalsIgnoreCase(formType))
        {
            if (!args.has("particle_key"))
            {
                return null;
            }

            ParticleForm form = new ParticleForm();
            form.effect.set(AIToolUtils.normalizeParticleKey(args.get("particle_key").getAsString()));

            if (args.has("texture"))
            {
                form.texture.set(Link.create(AIToolUtils.normalizeAssetLink(args.get("texture").getAsString())));
            }

            if (args.has("display_name"))
            {
                form.name.set(args.get("display_name").getAsString());
            }

            return form;
        }

        if ("label".equalsIgnoreCase(formType))
        {
            LabelForm form = new LabelForm();
            form.text.set(args.has("text") ? args.get("text").getAsString() : "Label");

            if (args.has("display_name"))
            {
                form.name.set(args.get("display_name").getAsString());
            }

            if (args.has("billboard"))
            {
                form.billboard.set(args.get("billboard").getAsBoolean());
            }

            if (args.has("color_rgb"))
            {
                form.color.set(Color.rgba(args.get("color_rgb").getAsInt()));
            }

            return form;
        }

        if ("billboard".equalsIgnoreCase(formType))
        {
            if (!args.has("texture"))
            {
                return null;
            }

            BillboardForm form = new BillboardForm();
            form.texture.set(Link.create(AIToolUtils.normalizeAssetLink(args.get("texture").getAsString())));

            if (args.has("display_name"))
            {
                form.name.set(args.get("display_name").getAsString());
            }

            if (args.has("billboard"))
            {
                form.billboard.set(args.get("billboard").getAsBoolean());
            }

            if (args.has("color_rgb"))
            {
                form.color.set(Color.rgba(args.get("color_rgb").getAsInt()));
            }

            return form;
        }

        if ("mob".equalsIgnoreCase(formType))
        {
            MobForm form = new MobForm();
            form.mobID.set(args.has("mob_id") ? args.get("mob_id").getAsString() : "minecraft:player");

            if (args.has("texture"))
            {
                form.texture.set(Link.create(AIToolUtils.normalizeAssetLink(args.get("texture").getAsString())));
            }

            if (args.has("display_name"))
            {
                form.name.set(args.get("display_name").getAsString());
            }

            return form;
        }

        return null;
    }
}
