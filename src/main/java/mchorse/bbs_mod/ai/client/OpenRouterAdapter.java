package mchorse.bbs_mod.ai.client;

/**
 * OpenRouter uses OpenAI-compatible API format with a different base URL.
 */
public class OpenRouterAdapter extends OpenAIAdapter
{
    private static final String DEFAULT_URL = "https://openrouter.ai/api/v1/chat/completions";

    @Override
    public String getApiUrl(String baseUrl)
    {
        return (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : DEFAULT_URL;
    }
}
