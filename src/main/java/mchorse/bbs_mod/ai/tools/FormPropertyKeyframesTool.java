package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

public class FormPropertyKeyframesTool implements IAITool
{
    @Override
    public String getName()
    {
        return "form_property_keyframes";
    }

    @Override
    public String getDescription()
    {
        return "Animate any animatable form property over time using exact property paths from form_inspect. This is how the AI should animate model/form sub-settings instead of only setting static values.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"actor_index\":{\"type\":\"integer\",\"description\":\"Actor index from actor_list\"}," +
            "\"path\":{\"type\":\"string\",\"description\":\"Exact animatable property path from form_inspect\"}," +
            "\"keyframes\":{\"type\":\"array\",\"description\":\"Keyframes for the property\",\"items\":{\"type\":\"object\",\"properties\":{" +
            "\"tick\":{\"type\":\"integer\"}," +
            "\"value\":{\"description\":\"Property value as JSON\"}," +
            "\"interpolation\":{\"type\":\"string\"},\"interpolation_args\":{\"type\":\"array\",\"items\":{\"type\":\"number\"}}," +
            "\"duration\":{\"type\":\"number\"},\"lx\":{\"type\":\"number\"},\"ly\":{\"type\":\"number\"},\"rx\":{\"type\":\"number\"},\"ry\":{\"type\":\"number\"}" +
            "},\"required\":[\"tick\",\"value\"]}}," +
            "\"clear_existing\":{\"type\":\"boolean\",\"description\":\"Clear existing channel before inserting\"}" +
            "},\"required\":[\"actor_index\",\"path\",\"keyframes\"]}";
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
            String path = args.get("path").getAsString();
            Replay replay = AIToolUtils.requireReplay(film, actorIndex);

            if (replay == null)
            {
                return AIToolResult.error("Actor index " + actorIndex + " is out of range. Use actor_list first.");
            }

            Form form = replay.form.get();

            if (form == null)
            {
                return AIToolResult.error("Actor \"" + replay.getName() + "\" has no form assigned.");
            }

            KeyframeChannel<?> channel = replay.properties.getOrCreate(form, path);

            if (channel == null)
            {
                return AIToolResult.error("Property path \"" + path + "\" is not animatable or does not exist. Use form_inspect first.");
            }

            JsonArray keyframes = args.getAsJsonArray("keyframes");

            if (keyframes == null || keyframes.size() == 0)
            {
                return AIToolResult.error("No property keyframes were provided.");
            }

            boolean clearExisting = args.has("clear_existing") && args.get("clear_existing").getAsBoolean();
            final int[] changes = {0};

            BaseValue.edit(film, (f) ->
            {
                if (clearExisting)
                {
                    channel.removeAll();
                }

                for (JsonElement element : keyframes)
                {
                    JsonObject json = element.getAsJsonObject();
                    int tick = json.get("tick").getAsInt();
                    if (tick < 0) continue;

                    BaseType value = AIToolUtils.jsonToData(json.get("value"));

                    if (value == null)
                    {
                        continue;
                    }

                    @SuppressWarnings("rawtypes")
                    KeyframeChannel raw = channel;
                    Object typedValue = raw.getFactory().fromData(value);
                    int index = raw.insert(tick, typedValue);
                    AIToolUtils.applyKeyframeMeta(channel, index, json);
                    changes[0] += 1;
                }
            });

            if (changes[0] == 0)
            {
                return AIToolResult.error("No property keyframes were inserted for \"" + path + "\".");
            }

            return AIToolResult.success("Inserted " + changes[0] + " keyframe(s) for property \"" + path + "\" on actor \"" + replay.getName() + "\"", changes[0]);
        }
        catch (Exception e)
        {
            return AIToolResult.error("Failed to animate form property: " + e.getMessage());
        }
    }
}
