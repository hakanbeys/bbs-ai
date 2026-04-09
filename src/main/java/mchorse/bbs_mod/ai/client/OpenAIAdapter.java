package mchorse.bbs_mod.ai.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class OpenAIAdapter implements AIProviderAdapter
{
    private static final String DEFAULT_URL = "https://api.openai.com/v1/chat/completions";

    @Override
    public String getApiUrl(String baseUrl)
    {
        if (baseUrl != null && !baseUrl.isEmpty())
        {
            return baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
        }
        return DEFAULT_URL;
    }

    @Override
    public String formatRequestBody(AIRequest request)
    {
        JsonObject body = new JsonObject();
        body.addProperty("model", request.getModel());
        body.addProperty("max_tokens", request.getMaxTokens());
        body.addProperty("temperature", request.getTemperature());

        JsonArray messages = new JsonArray();
        for (AIMessage msg : request.getMessages())
        {
            JsonObject msgObj = new JsonObject();

            switch (msg.getRole())
            {
                case SYSTEM:
                    msgObj.addProperty("role", "system");
                    msgObj.addProperty("content", msg.getContent());
                    break;

                case USER:
                    msgObj.addProperty("role", "user");
                    if (msg.hasImage())
                    {
                        JsonArray content = new JsonArray();
                        JsonObject textPart = new JsonObject();
                        textPart.addProperty("type", "text");
                        textPart.addProperty("text", msg.getContent());
                        content.add(textPart);

                        JsonObject imagePart = new JsonObject();
                        imagePart.addProperty("type", "image_url");
                        JsonObject imageUrl = new JsonObject();
                        imageUrl.addProperty("url", "data:" + msg.getImageMimeType() + ";base64," + msg.getImageBase64());
                        imagePart.add("image_url", imageUrl);
                        content.add(imagePart);

                        msgObj.add("content", content);
                    }
                    else
                    {
                        msgObj.addProperty("content", msg.getContent());
                    }
                    break;

                case ASSISTANT:
                    msgObj.addProperty("role", "assistant");
                    msgObj.addProperty("content", msg.getContent());
                    break;

                case TOOL:
                    msgObj.addProperty("role", "tool");
                    msgObj.addProperty("tool_call_id", msg.getToolCallId());
                    msgObj.addProperty("content", msg.getContent());
                    break;
            }

            messages.add(msgObj);
        }

        body.add("messages", messages);

        if (request.hasTools())
        {
            JsonArray tools = new JsonArray();
            for (AIRequest.AIToolDefinition tool : request.getTools())
            {
                JsonObject toolObj = new JsonObject();
                toolObj.addProperty("type", "function");

                JsonObject function = new JsonObject();
                function.addProperty("name", tool.name);
                function.addProperty("description", tool.description);

                if (tool.parametersJson != null && !tool.parametersJson.isEmpty())
                {
                    function.add("parameters", JsonParser.parseString(tool.parametersJson));
                }

                toolObj.add("function", function);
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

            if (json.has("usage"))
            {
                JsonObject usage = json.getAsJsonObject("usage");
                response.setPromptTokens(usage.has("prompt_tokens") ? usage.get("prompt_tokens").getAsInt() : 0);
                response.setCompletionTokens(usage.has("completion_tokens") ? usage.get("completion_tokens").getAsInt() : 0);
            }

            if (json.has("choices"))
            {
                JsonArray choices = json.getAsJsonArray("choices");
                if (choices.size() > 0)
                {
                    JsonObject choice = choices.get(0).getAsJsonObject();
                    response.setFinishReason(choice.has("finish_reason") ? choice.get("finish_reason").getAsString() : "stop");

                    JsonObject message = choice.getAsJsonObject("message");
                    if (message.has("content") && !message.get("content").isJsonNull())
                    {
                        response.setContent(message.get("content").getAsString());
                    }

                    if (message.has("tool_calls"))
                    {
                        JsonArray toolCalls = message.getAsJsonArray("tool_calls");
                        for (JsonElement tc : toolCalls)
                        {
                            JsonObject toolCall = tc.getAsJsonObject();
                            String id = toolCall.get("id").getAsString();
                            JsonObject function = toolCall.getAsJsonObject("function");
                            String name = function.get("name").getAsString();
                            String args = function.has("arguments") ? function.get("arguments").getAsString() : "{}";
                            response.addToolCall(new AIToolCall(id, name, args));
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            response.setError("Failed to parse OpenAI response: " + e.getMessage());
        }

        return response;
    }

    @Override
    public String getAuthHeader(String apiKey)
    {
        return "Bearer " + apiKey;
    }

    @Override
    public String getAuthHeaderName()
    {
        return "Authorization";
    }

    @Override
    public boolean supportsVision()
    {
        return true;
    }
}
