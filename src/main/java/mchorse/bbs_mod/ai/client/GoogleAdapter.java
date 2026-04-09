package mchorse.bbs_mod.ai.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GoogleAdapter implements AIProviderAdapter
{
    @Override
    public String getApiUrl(String baseUrl)
    {
        /* Google uses model in URL, handled by AIHttpClient */
        return "https://generativelanguage.googleapis.com/v1beta/models/";
    }

    public String getApiUrl(String baseUrl, String model, String apiKey)
    {
        if (baseUrl != null && !baseUrl.isEmpty())
        {
            return baseUrl;
        }
        return "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
    }

    @Override
    public String formatRequestBody(AIRequest request)
    {
        JsonObject body = new JsonObject();

        JsonArray contents = new JsonArray();
        String systemText = null;

        for (AIMessage msg : request.getMessages())
        {
            if (msg.getRole() == AIMessage.Role.SYSTEM)
            {
                systemText = msg.getContent();
                continue;
            }

            JsonObject content = new JsonObject();
            String role = msg.getRole() == AIMessage.Role.ASSISTANT ? "model" : "user";
            content.addProperty("role", role);

            JsonArray parts = new JsonArray();

            if (msg.getRole() == AIMessage.Role.TOOL)
            {
                JsonObject functionResponse = new JsonObject();
                functionResponse.addProperty("name", msg.getToolName());
                JsonObject responseObj = new JsonObject();
                responseObj.addProperty("result", msg.getContent());
                functionResponse.add("response", responseObj);
                JsonObject part = new JsonObject();
                part.add("functionResponse", functionResponse);
                parts.add(part);
            }
            else
            {
                if (msg.hasImage())
                {
                    JsonObject imagePart = new JsonObject();
                    JsonObject inlineData = new JsonObject();
                    inlineData.addProperty("mimeType", msg.getImageMimeType());
                    inlineData.addProperty("data", msg.getImageBase64());
                    imagePart.add("inlineData", inlineData);
                    parts.add(imagePart);
                }

                JsonObject textPart = new JsonObject();
                textPart.addProperty("text", msg.getContent());
                parts.add(textPart);
            }

            content.add("parts", parts);
            contents.add(content);
        }

        body.add("contents", contents);

        if (systemText != null)
        {
            JsonObject systemInstruction = new JsonObject();
            JsonArray sysParts = new JsonArray();
            JsonObject sysPart = new JsonObject();
            sysPart.addProperty("text", systemText);
            sysParts.add(sysPart);
            systemInstruction.add("parts", sysParts);
            body.add("systemInstruction", systemInstruction);
        }

        /* Tools */
        if (request.hasTools())
        {
            JsonArray tools = new JsonArray();
            JsonObject toolObj = new JsonObject();
            JsonArray functionDeclarations = new JsonArray();

            for (AIRequest.AIToolDefinition tool : request.getTools())
            {
                JsonObject funcDecl = new JsonObject();
                funcDecl.addProperty("name", tool.name);
                funcDecl.addProperty("description", tool.description);
                if (tool.parametersJson != null && !tool.parametersJson.isEmpty())
                {
                    funcDecl.add("parameters", JsonParser.parseString(tool.parametersJson));
                }
                functionDeclarations.add(funcDecl);
            }

            toolObj.add("functionDeclarations", functionDeclarations);
            tools.add(toolObj);
            body.add("tools", tools);
        }

        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("maxOutputTokens", request.getMaxTokens());
        genConfig.addProperty("temperature", request.getTemperature());
        body.add("generationConfig", genConfig);

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

            if (json.has("candidates"))
            {
                JsonArray candidates = json.getAsJsonArray("candidates");
                if (candidates.size() > 0)
                {
                    JsonObject candidate = candidates.get(0).getAsJsonObject();
                    JsonObject content = candidate.getAsJsonObject("content");

                    if (content != null && content.has("parts"))
                    {
                        JsonArray parts = content.getAsJsonArray("parts");
                        StringBuilder textContent = new StringBuilder();

                        for (JsonElement part : parts)
                        {
                            JsonObject partObj = part.getAsJsonObject();

                            if (partObj.has("text"))
                            {
                                textContent.append(partObj.get("text").getAsString());
                            }
                            else if (partObj.has("functionCall"))
                            {
                                JsonObject functionCall = partObj.getAsJsonObject("functionCall");
                                String name = functionCall.get("name").getAsString();
                                String args = functionCall.has("args") ? functionCall.get("args").toString() : "{}";
                                response.addToolCall(new AIToolCall("call_" + name + "_" + System.nanoTime(), name, args));
                            }
                        }

                        if (textContent.length() > 0)
                        {
                            response.setContent(textContent.toString());
                        }
                    }

                    response.setFinishReason(candidate.has("finishReason") ? candidate.get("finishReason").getAsString() : "STOP");
                }
            }

            if (json.has("usageMetadata"))
            {
                JsonObject usage = json.getAsJsonObject("usageMetadata");
                response.setPromptTokens(usage.has("promptTokenCount") ? usage.get("promptTokenCount").getAsInt() : 0);
                response.setCompletionTokens(usage.has("candidatesTokenCount") ? usage.get("candidatesTokenCount").getAsInt() : 0);
            }
        }
        catch (Exception e)
        {
            response.setError("Failed to parse Google response: " + e.getMessage());
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
        return null; /* Google uses key in URL param */
    }

    @Override
    public boolean supportsVision()
    {
        return true;
    }
}
