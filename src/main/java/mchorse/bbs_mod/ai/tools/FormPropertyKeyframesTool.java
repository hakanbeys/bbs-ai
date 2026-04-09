package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
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
        return "Animate a form property over time using exact property paths from form_inspect.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"actor_index\":{\"type\":\"integer\",\"description\":\"Actor index\"}," +
            "\"property_path\":{\"type\":\"string\",\"description\":\"Exact property path\"}," +
            "\"keyframes\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{" +
            "\"tick\":{\"type\":\"integer\"},\"value\":{},\"interpolation\":{\"type\":\"string\"}" +
            "},\"required\":[\"tick\",\"value\"]}}" +
            "},\"required\":[\"actor_index\",\"property_path\",\"keyframes\"]}";
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
            String path = args.get("property_path").getAsString();
            JsonArray keyframes = args.getAsJsonArray("keyframes");

            Replay replay = AIToolUtils.getReplay(film, actorIndex);

            if (replay == null)
            {
                return AIToolResult.error(AIToolUtils.replayOutOfRangeMessage(film, actorIndex));
            }

            Form form = replay.form.get();

            if (form == null)
            {
                return AIToolResult.error("Actor \"" + replay.getName() + "\" has no form assigned.");
            }

            if (FormUtils.getProperty(form, path) == null)
            {
                return AIToolResult.error("Property not found: " + path);
            }

            KeyframeChannel channel = replay.properties.getOrCreate(form, path);

            if (channel == null)
            {
                return AIToolResult.error("Property is not animatable: " + path);
            }

            final int[] count = {0};

            BaseValue.edit(film, (f) ->
            {
                for (JsonElement element : keyframes)
                {
                    JsonObject keyframe = element.getAsJsonObject();
                    int tick = keyframe.get("tick").getAsInt();
                    Object value = channel.getFactory().fromData(DataToString.fromString(keyframe.get("value").toString()));
                    int index = channel.insert(tick, value);
                    count[0]++;
                    AIToolUtils.applyInterpolation(channel, index, keyframe, null);
                }
            });

            return AIToolResult.success("Inserted " + count[0] + " form property keyframes for " + path + " on \"" + replay.getName() + "\".", count[0]);
        }
        catch (Exception e)
        {
            return AIToolResult.error("Form property keyframes failed: " + e.getMessage());
        }
    }
}
