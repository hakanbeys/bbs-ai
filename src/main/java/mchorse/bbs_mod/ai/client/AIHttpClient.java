package mchorse.bbs_mod.ai.client;

import mchorse.bbs_mod.ai.AISettings;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AIHttpClient
{
    private static final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    private static final Map<String, AIProviderAdapter> adapters = new HashMap<>();

    static
    {
        adapters.put("anthropic", new AnthropicAdapter());
        adapters.put("openai", new OpenAIAdapter());
        adapters.put("google", new GoogleAdapter());
        adapters.put("openrouter", new OpenRouterAdapter());
    }

    public static AIProviderAdapter getAdapter(String provider)
    {
        AIProviderAdapter adapter = adapters.get(provider.toLowerCase());
        return adapter != null ? adapter : adapters.get("openai");
    }

    public static CompletableFuture<AIResponse> sendRequest(AIRequest request)
    {
        String provider = AISettings.aiProvider.get();
        String apiKey = AISettings.aiApiKey.get();
        String baseUrl = AISettings.aiBaseUrl.get();

        if (apiKey == null || apiKey.isEmpty())
        {
            return CompletableFuture.completedFuture(errorResponse("API key is not configured. Open AI Settings to add your key."));
        }

        AIProviderAdapter adapter = getAdapter(provider);
        String body = adapter.formatRequestBody(request);

        String url;
        if (adapter instanceof GoogleAdapter googleAdapter)
        {
            url = googleAdapter.getApiUrl(baseUrl, request.getModel(), apiKey);
        }
        else
        {
            url = adapter.getApiUrl(baseUrl);
        }

        HttpRequest.Builder httpBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(120))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body));

        /* Add auth header */
        String authHeaderName = adapter.getAuthHeaderName();
        if (authHeaderName != null)
        {
            httpBuilder.header(authHeaderName, adapter.getAuthHeader(apiKey));
        }

        /* Anthropic requires version header */
        if ("anthropic".equals(provider))
        {
            httpBuilder.header("anthropic-version", "2023-06-01");
        }

        return client.sendAsync(httpBuilder.build(), HttpResponse.BodyHandlers.ofString())
            .thenApply(response ->
            {
                if (response.statusCode() >= 400)
                {
                    AIResponse errResponse = adapter.parseResponse(response.body());
                    if (!errResponse.isError())
                    {
                        errResponse.setError("HTTP " + response.statusCode() + ": " + response.body());
                    }
                    return errResponse;
                }
                return adapter.parseResponse(response.body());
            })
            .exceptionally(throwable ->
            {
                return errorResponse("Network error: " + throwable.getMessage());
            });
    }

    private static AIResponse errorResponse(String message)
    {
        AIResponse response = new AIResponse();
        response.setError(message);
        return response;
    }
}
