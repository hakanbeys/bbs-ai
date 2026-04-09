package mchorse.bbs_mod.ai.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Fetches available model lists from AI providers.
 */
public class AIModelFetcher
{
    private static final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    /**
     * Fetch models for the given provider.
     */
    public static CompletableFuture<List<String>> fetchModels(String provider, String apiKey, String baseUrl)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            try
            {
                switch (provider.toLowerCase())
                {
                    case "openrouter": return fetchOpenRouterModels();
                    case "openai": return fetchOpenAIModels(apiKey, baseUrl);
                    case "anthropic": return getAnthropicModels();
                    case "google": return getGoogleModels();
                    default: return getAnthropicModels();
                }
            }
            catch (Exception e)
            {
                List<String> fallback = new ArrayList<>();
                fallback.add("Error: " + e.getMessage());
                return fallback;
            }
        });
    }

    private static List<String> fetchOpenRouterModels() throws Exception
    {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://openrouter.ai/api/v1/models"))
            .header("Accept", "application/json")
            .GET()
            .timeout(Duration.ofSeconds(15))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        List<String> models = new ArrayList<>();

        if (response.statusCode() == 200)
        {
            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray data = body.getAsJsonArray("data");

            for (JsonElement elem : data)
            {
                JsonObject model = elem.getAsJsonObject();
                String id = model.get("id").getAsString();
                models.add(id);
            }
        }

        if (models.isEmpty())
        {
            models.add("anthropic/claude-sonnet-4-20250514");
            models.add("openai/gpt-4o");
            models.add("google/gemini-2.0-flash-001");
        }

        return models;
    }

    private static List<String> fetchOpenAIModels(String apiKey, String baseUrl) throws Exception
    {
        String url = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : "https://api.openai.com/v1";
        if (!url.endsWith("/models")) url += "/models";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .timeout(Duration.ofSeconds(15));

        if (apiKey != null && !apiKey.isEmpty())
        {
            builder.header("Authorization", "Bearer " + apiKey);
        }

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        List<String> models = new ArrayList<>();

        if (response.statusCode() == 200)
        {
            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray data = body.getAsJsonArray("data");

            for (JsonElement elem : data)
            {
                JsonObject model = elem.getAsJsonObject();
                String id = model.get("id").getAsString();
                if (id.startsWith("gpt-") || id.startsWith("o1") || id.startsWith("o3") || id.startsWith("o4"))
                {
                    models.add(id);
                }
            }
        }

        if (models.isEmpty())
        {
            models.add("gpt-4o");
            models.add("gpt-4o-mini");
            models.add("o4-mini");
        }

        return models;
    }

    private static List<String> getAnthropicModels()
    {
        List<String> models = new ArrayList<>();
        models.add("claude-opus-4-20250514");
        models.add("claude-sonnet-4-20250514");
        models.add("claude-haiku-4-20250414");
        models.add("claude-3-5-sonnet-20241022");
        models.add("claude-3-5-haiku-20241022");
        return models;
    }

    private static List<String> getGoogleModels()
    {
        List<String> models = new ArrayList<>();
        models.add("gemini-2.5-pro-preview-06-05");
        models.add("gemini-2.5-flash-preview-05-20");
        models.add("gemini-2.0-flash-001");
        models.add("gemini-2.0-flash-lite-001");
        return models;
    }
}
