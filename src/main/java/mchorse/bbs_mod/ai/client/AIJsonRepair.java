package mchorse.bbs_mod.ai.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AIJsonRepair
{
    public static String repairObject(String raw)
    {
        if (raw == null || raw.trim().isEmpty())
        {
            return "{}";
        }

        String input = raw.trim();

        if (!input.startsWith("{"))
        {
            int start = input.indexOf('{');

            if (start >= 0)
            {
                input = input.substring(start);
            }
        }

        String direct = tryParseObject(input);

        if (direct != null)
        {
            return direct;
        }

        for (int cut = input.length(); cut > 0 && cut >= input.length() - 256; cut--)
        {
            String candidate = sanitizeTail(input.substring(0, cut));
            String repaired = closeJson(candidate);
            String parsed = tryParseObject(repaired);

            if (parsed != null)
            {
                return parsed;
            }
        }

        return "{}";
    }

    public static JsonObject parseObjectOrFallback(String raw)
    {
        try
        {
            return JsonParser.parseString(repairObject(raw)).getAsJsonObject();
        }
        catch (Exception e)
        {
            return new JsonObject();
        }
    }

    private static String tryParseObject(String text)
    {
        try
        {
            JsonElement element = JsonParser.parseString(text);
            return element.isJsonObject() ? element.getAsJsonObject().toString() : null;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private static String sanitizeTail(String text)
    {
        String candidate = text.trim();

        while (!candidate.isEmpty())
        {
            char last = candidate.charAt(candidate.length() - 1);

            if (last == ',' || last == ':' || Character.isWhitespace(last))
            {
                candidate = candidate.substring(0, candidate.length() - 1).trim();
                continue;
            }

            if (last == '"' && hasUnclosedQuote(candidate))
            {
                candidate = candidate.substring(0, candidate.length() - 1).trim();
                continue;
            }

            break;
        }

        return candidate;
    }

    private static String closeJson(String text)
    {
        StringBuilder builder = new StringBuilder(text);
        int braces = 0;
        int brackets = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < builder.length(); i++)
        {
            char c = builder.charAt(i);

            if (escaped)
            {
                escaped = false;
                continue;
            }

            if (c == '\\')
            {
                escaped = true;
                continue;
            }

            if (c == '"')
            {
                inString = !inString;
                continue;
            }

            if (!inString)
            {
                if (c == '{') braces++;
                else if (c == '}') braces = Math.max(0, braces - 1);
                else if (c == '[') brackets++;
                else if (c == ']') brackets = Math.max(0, brackets - 1);
            }
        }

        if (inString)
        {
            builder.append('"');
        }

        while (brackets-- > 0)
        {
            builder.append(']');
        }

        while (braces-- > 0)
        {
            builder.append('}');
        }

        return builder.toString().replaceAll(",\\s*([}\\]])", "$1");
    }

    private static boolean hasUnclosedQuote(String text)
    {
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < text.length(); i++)
        {
            char c = text.charAt(i);

            if (escaped)
            {
                escaped = false;
                continue;
            }

            if (c == '\\')
            {
                escaped = true;
                continue;
            }

            if (c == '"')
            {
                inString = !inString;
            }
        }

        return inString;
    }
}
