package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;

import java.util.List;

/**
 * Controls form (model) properties on actors.
 * Can list all animatable properties, get/set their values,
 * and create keyframe channels for form properties.
 */
public class FormTool implements IAITool
{
    @Override
    public String getName()
    {
        return "form_tool";
    }

    @Override
    public String getDescription()
    {
        return "Control model/form properties on actors. Can list all animatable properties of a form, " +
            "read current values, and set form property keyframes for animation. " +
            "Use action 'list' to see available properties, 'get' to read a value, 'set' to set a property value.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"actor_index\":{\"type\":\"integer\",\"description\":\"Index of the actor\"}," +
            "\"action\":{\"type\":\"string\",\"enum\":[\"list\",\"get\",\"set\",\"info\"],\"description\":\"Action: list properties, get value, set value, or info about the form\"}," +
            "\"property_path\":{\"type\":\"string\",\"description\":\"Property path (e.g. 'tx', 'ry', 'sx'). Required for get/set.\"}," +
            "\"value\":{\"type\":\"number\",\"description\":\"Value to set. Required for 'set' action.\"}," +
            "\"tick\":{\"type\":\"integer\",\"description\":\"Tick for keyframe. If omitted for 'set', sets at tick 0.\"}" +
            "},\"required\":[\"actor_index\",\"action\"]}";
    }

    @Override
    public AIToolResult execute(String argumentsJson, AIToolContext context)
    {
        Film film = context.getFilm();
        if (film == null) return AIToolResult.error("No film is currently open.");

        try
        {
            JsonObject args = JsonParser.parseString(argumentsJson).getAsJsonObject();
            int actorIndex = args.get("actor_index").getAsInt();
            String action = args.get("action").getAsString();

            List<Replay> replays = film.replays.getList();
            if (actorIndex < 0 || actorIndex >= replays.size())
            {
                return AIToolResult.error("Actor index " + actorIndex + " is out of range. Max: " + (replays.size() - 1));
            }

            Replay replay = replays.get(actorIndex);
            Form form = replay.form.get();

            if (form == null)
            {
                return AIToolResult.error("Actor \"" + replay.getName() + "\" has no form/model assigned. Assign a model first.");
            }

            switch (action)
            {
                case "info": return getFormInfo(replay, form);
                case "list": return listProperties(form);
                case "get": return getProperty(form, args);
                case "set": return setProperty(film, replay, form, args);
                default: return AIToolResult.error("Unknown action: " + action);
            }
        }
        catch (Exception e)
        {
            return AIToolResult.error("Form tool failed: " + e.getMessage());
        }
    }

    private AIToolResult getFormInfo(Replay replay, Form form)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Actor: \"").append(replay.getName()).append("\"\n");
        sb.append("Form type: ").append(form.getClass().getSimpleName()).append("\n");
        sb.append("Display name: ").append(form.getDisplayName()).append("\n");
        sb.append("Animatable: ").append(form.animatable.get()).append("\n");

        List<String> props = FormUtils.collectPropertyPaths(form);
        sb.append("Animatable properties: ").append(props.size()).append("\n");
        sb.append("Use 'list' action to see all property paths.\n");

        return AIToolResult.success(sb.toString());
    }

    private AIToolResult listProperties(Form form)
    {
        List<String> props = FormUtils.collectPropertyPaths(form);

        if (props.isEmpty())
        {
            return AIToolResult.success("No animatable properties found. The form might not be animatable.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Animatable properties (").append(props.size()).append("):\n");

        for (String prop : props)
        {
            BaseValueBasic value = FormUtils.getProperty(form, prop);
            String currentValue = value != null ? String.valueOf(value.get()) : "?";
            sb.append("  ").append(prop).append(" = ").append(currentValue).append("\n");
        }

        return AIToolResult.success(sb.toString());
    }

    private AIToolResult getProperty(Form form, JsonObject args)
    {
        if (!args.has("property_path"))
        {
            return AIToolResult.error("property_path is required for 'get' action.");
        }

        String path = args.get("property_path").getAsString();
        BaseValueBasic value = FormUtils.getProperty(form, path);

        if (value == null)
        {
            return AIToolResult.error("Property not found: " + path);
        }

        return AIToolResult.success(path + " = " + value.get());
    }

    private AIToolResult setProperty(Film film, Replay replay, Form form, JsonObject args)
    {
        if (!args.has("property_path"))
        {
            return AIToolResult.error("property_path is required for 'set' action.");
        }
        if (!args.has("value"))
        {
            return AIToolResult.error("value is required for 'set' action.");
        }

        String path = args.get("property_path").getAsString();
        float value = args.get("value").getAsFloat();
        int tick = args.has("tick") ? args.get("tick").getAsInt() : 0;

        /* Create/get keyframe channel for this property */
        var channel = replay.properties.getOrCreate(form, path);

        if (channel == null)
        {
            return AIToolResult.error("Cannot create keyframe for property: " + path + ". It may not be animatable.");
        }

        BaseValue.edit(film, (f) ->
        {
            channel.insert(tick, value);
        });

        return AIToolResult.success(
            "Set " + path + " = " + value + " at tick " + tick + " on \"" + replay.getName() + "\"",
            1
        );
    }
}
