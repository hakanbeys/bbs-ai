package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.ai.AISettings;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Searches the web using Tavily or Brave Search API.
 * Useful for looking up animation references, techniques, etc.
 */
public class WebSearchTool implements IAITool
{
    private static final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    @Override
    public String getName()
    {
        return "web_search";
    }

    @Override
    public String getDescription()
    {
        return "Search the web for animation references, techniques, or other information. " +
            "Requires a web search API key to be configured in settings.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"query\":{\"type\":\"string\",\"description\":\"Search query\"}," +
            "\"max_results\":{\"type\":\"integer\",\"description\":\"Maximum number of results (default: 5)\"}" +
            "},\"required\":[\"query\"]}";
    }

    @Override
    public AIToolResult execute(String argumentsJson, AIToolContext context)
    {
        String apiKey = AISettings.webSearchApiKey().get();
        if (apiKey == null || apiKey.trim().isEmpty())
        {
            return AIToolResult.error("Web search API key is not configured. Set it in AI settings.");
        }

        try
        {
            JsonObject args = JsonParser.parseString(argumentsJson).getAsJsonObject();
            String query = args.get("query").getAsString();
            int maxResults = args.has("max_results") ? args.get("max_results").getAsInt() : 5;

            String provider = AISettings.webSearchProvider().get();

            if ("tavily".equalsIgnoreCase(provider))
            {
                return searchTavily(query, maxResults, apiKey);
            }
            else if ("brave".equalsIgnoreCase(provider))
            {
                return searchBrave(query, maxResults, apiKey);
            }
            else
            {
                return AIToolResult.error("Unknown web search provider: " + provider + ". Use 'tavily' or 'brave'.");
            }
        }
        catch (Exception e)
        {
            return AIToolResult.error("Web search failed: " + e.getMessage());
        }
    }

    private AIToolResult searchTavily(String query, int maxResults, String apiKey)
    {
        try
        {
            JsonObject body = new JsonObject();
            body.addProperty("api_key", apiKey);
            body.addProperty("query", query);
            body.addProperty("max_results", maxResults);
            body.addProperty("include_answer", true);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.tavily.com/search"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .timeout(Duration.ofSeconds(15))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200)
            {
                return AIToolResult.error("Tavily API error (HTTP " + response.statusCode() + "): " + response.body());
            }

            JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
            StringBuilder sb = new StringBuilder();

            if (result.has("answer") && !result.get("answer").isJsonNull())
            {
                sb.append("Answer: ").append(result.get("answer").getAsString()).append("\n\n");
            }

            if (result.has("results"))
            {
                JsonArray results = result.getAsJsonArray("results");
                sb.append("Results (").append(results.size()).append("):\n");

                for (JsonElement elem : results)
                {
                    JsonObject r = elem.getAsJsonObject();
                    sb.append("- ").append(r.get("title").getAsString()).append("\n");
                    sb.append("  URL: ").append(r.get("url").getAsString()).append("\n");
                    if (r.has("content") && !r.get("content").isJsonNull())
                    {
                        String content = r.get("content").getAsString();
                        if (content.length() > 200)
                        {
                            content = content.substring(0, 200) + "...";
                        }
                        sb.append("  ").append(content).append("\n");
                    }
                    sb.append("\n");
                }
            }

            return AIToolResult.success(sb.toString());
        }
        catch (Exception e)
        {
            return AIToolResult.error("Tavily search failed: " + e.getMessage());
        }
    }

    private AIToolResult searchBrave(String query, int maxResults, String apiKey)
    {
        try
        {
            String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
            String url = "https://api.search.brave.com/res/v1/web/search?q=" + encodedQuery + "&count=" + maxResults;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Accept-Encoding", "gzip")
                .header("X-Subscription-Token", apiKey)
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200)
            {
                return AIToolResult.error("Brave API error (HTTP " + response.statusCode() + "): " + response.body());
            }

            JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
            StringBuilder sb = new StringBuilder();

            if (result.has("web") && result.getAsJsonObject("web").has("results"))
            {
                JsonArray results = result.getAsJsonObject("web").getAsJsonArray("results");
                sb.append("Results (").append(results.size()).append("):\n");

                for (JsonElement elem : results)
                {
                    JsonObject r = elem.getAsJsonObject();
                    sb.append("- ").append(r.get("title").getAsString()).append("\n");
                    sb.append("  URL: ").append(r.get("url").getAsString()).append("\n");
                    if (r.has("description") && !r.get("description").isJsonNull())
                    {
                        String desc = r.get("description").getAsString();
                        if (desc.length() > 200)
                        {
                            desc = desc.substring(0, 200) + "...";
                        }
                        sb.append("  ").append(desc).append("\n");
                    }
                    sb.append("\n");
                }
            }
            else
            {
                sb.append("No results found for: ").append(query);
            }

            return AIToolResult.success(sb.toString());
        }
        catch (Exception e)
        {
            return AIToolResult.error("Brave search failed: " + e.getMessage());
        }
    }
}
