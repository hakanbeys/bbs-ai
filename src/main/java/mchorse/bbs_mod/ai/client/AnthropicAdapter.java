package mchorse.bbs_mod.ai.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AnthropicAdapter implements AIProviderAdapter
{
    private static final String DEFAULT_URL = "https://api.anthropic.com/v1/messages";

    @Override
    public String getApiUrl(String baseUrl)
    {
        return (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : DEFAULT_URL;
    }

    @Override
    public String formatRequestBody(AIRequest request)
    {
        JsonObject body = new JsonObject();
        body.addProperty("model", request.getModel());
        body.addProperty("max_tokens", request.getMaxTokens());
        body.addProperty("temperature", request.getTemperature());

        /* Extract system message */
        StringBuilder system = new StringBuilder();
        JsonArray messages = new JsonArray();
        for (AIMessage msg : request.getMessages())
        {
            if (msg.getRole() == AIMessage.Role.SYSTEM)
            {
                if (system.length() > 0)
                {
                    system.append("\n\n");
                }

                system.append(msg.getContent());
                continue;
            }

            JsonObject msgObj = new JsonObject();

            switch (msg.getRole())
            {
                case USER:
                    msgObj.addProperty("role", "user");
                    if (msg.hasImage())
                    {
                        JsonArray content = new JsonArray();
                        JsonObject imageBlock = new JsonObject();
                        imageBlock.addProperty("type", "image");
                        JsonObject source = new JsonObject();
                        source.addProperty("type", "base64");
                        source.addProperty("media_type", msg.getImageMimeType());
                        source.addProperty("data", msg.getImageBase64());
                        imageBlock.add("source", source);
                        content.add(imageBlock);

                        JsonObject textBlock = new JsonObject();
                        textBlock.addProperty("type", "text");
                        textBlock.addProperty("text", msg.getContent());
                        content.add(textBlock);

                        msgObj.add("content", content);
                    }
                    else
                    {
                        msgObj.addProperty("content", msg.getContent());
                    }
                    break;

                case ASSISTANT:
                    msgObj.addProperty("role", "assistant");
                    if (msg.hasAssistantToolCalls())
                    {
                        JsonArray assistantContent = new JsonArray();

                        if (msg.getContent() != null && !msg.getContent().isEmpty())
                        {
                            JsonObject textBlock = new JsonObject();
                            textBlock.addProperty("type", "text");
                            textBlock.addProperty("text", msg.getContent());
                            assistantContent.add(textBlock);
                        }

                        for (AIToolCall toolCall : msg.getAssistantToolCalls())
                        {
                            JsonObject toolUse = new JsonObject();
                            toolUse.addProperty("type", "tool_use");
                            toolUse.addProperty("id", toolCall.getId());
                            toolUse.addProperty("name", toolCall.getFunctionName());
                            toolUse.add("input", JsonParser.parseString(toolCall.getArgumentsJson()));
                            assistantContent.add(toolUse);
                        }

                        msgObj.add("content", assistantContent);
                    }
                    else
                    {
                        msgObj.addProperty("content", msg.getContent());
                    }
                    break;

                case TOOL:
                    msgObj.addProperty("role", "user");
                    JsonArray toolContent = new JsonArray();
                    JsonObject toolResult = new JsonObject();
                    toolResult.addProperty("type", "tool_result");
                    toolResult.addProperty("tool_use_id", msg.getToolCallId());
                    toolResult.addProperty("content", msg.getContent());
                    toolContent.add(toolResult);
                    msgObj.add("content", toolContent);
                    break;

                default:
                    continue;
            }

            messages.add(msgObj);
        }

        if (system.length() > 0)
        {
            body.addProperty("system", system.toString());
        }

        body.add("messages", messages);

        /* Tools */
        if (request.hasTools())
        {
            JsonArray tools = new JsonArray();
            for (AIRequest.AIToolDefinition tool : request.getTools())
            {
                JsonObject toolObj = new JsonObject();
                toolObj.addProperty("name", tool.name);
                toolObj.addProperty("description", tool.description);

                if (tool.parametersJson != null && !tool.parametersJson.isEmpty())
                {
                    toolObj.add("input_schema", JsonParser.parseString(tool.parametersJson));
                }

                tools.add(toolObj);
            }
            body.add("tools", tools);
        }

        return body.toString();
    }

    @Override
    public AIResponse parseResponse(String responseBody)
    {
        AIResponse response = new AIResponse();

        try
        {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            if (json.has("error"))
            {
                JsonObject error = json.getAsJsonObject("error");
                response.setError(error.get("message").getAsString());
                return response;
            }

            response.setFinishReason(json.has("stop_reason") ? json.get("stop_reason").getAsString() : "end_turn");

            if (json.has("usage"))
            {
                JsonObject usage = json.getAsJsonObject("usage");
                response.setPromptTokens(usage.has("input_tokens") ? usage.get("input_tokens").getAsInt() : 0);
                response.setCompletionTokens(usage.has("output_tokens") ? usage.get("output_tokens").getAsInt() : 0);
            }

            if (json.has("content"))
            {
                JsonArray content = json.getAsJsonArray("content");
                StringBuilder textContent = new StringBuilder();

                for (JsonElement element : content)
                {
                    JsonObject block = element.getAsJsonObject();
                    String type = block.get("type").getAsString();

                    if ("text".equals(type))
                    {
                        textContent.append(block.get("text").getAsString());
                    }
                    else if ("tool_use".equals(type))
                    {
                        String id = block.get("id").getAsString();
                        String name = block.get("name").getAsString();
                        String args = block.has("input") ? block.get("input").toString() : "{}";
                        response.addToolCall(new AIToolCall(id, name, args));
                    }
                }

                if (textContent.length() > 0)
                {
                    response.setContent(textContent.toString());
                }
            }
        }
        catch (Exception e)
        {
            response.setError("Failed to parse Anthropic response: " + e.getMessage());
        }

        return response;
    }

    @Override
    public String getAuthHeader(String apiKey)
    {
        return apiKey;
    }

    @Override
    public String getAuthHeaderName()
    {
        return "x-api-key";
    }

    @Override
    public boolean supportsVision()
    {
        return true;
    }
}
