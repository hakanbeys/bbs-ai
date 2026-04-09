package mchorse.bbs_mod.ai.client;

public interface AIProviderAdapter
{
    String getApiUrl(String baseUrl);

    String formatRequestBody(AIRequest request);

    AIResponse parseResponse(String responseBody);

    String getAuthHeader(String apiKey);

    String getAuthHeaderName();

    boolean supportsVision();
}
