package mchorse.bbs_mod.ai.client;

import java.util.ArrayList;
import java.util.List;

public class AIRequest
{
    private String model;
    private List<AIMessage> messages = new ArrayList<>();
    private List<AIToolDefinition> tools = new ArrayList<>();
    private int maxTokens = 4096;
    private float temperature = 0.7f;

    public AIRequest(String model)
    {
        this.model = model;
    }

    public String getModel()
    {
        return this.model;
    }

    public List<AIMessage> getMessages()
    {
        return this.messages;
    }

    public void addMessage(AIMessage message)
    {
        this.messages.add(message);
    }

    public List<AIToolDefinition> getTools()
    {
        return this.tools;
    }

    public void addTool(AIToolDefinition tool)
    {
        this.tools.add(tool);
    }

    public int getMaxTokens()
    {
        return this.maxTokens;
    }

    public void setMaxTokens(int maxTokens)
    {
        this.maxTokens = maxTokens;
    }

    public float getTemperature()
    {
        return this.temperature;
    }

    public void setTemperature(float temperature)
    {
        this.temperature = temperature;
    }

    public boolean hasTools()
    {
        return !this.tools.isEmpty();
    }

    public static class AIToolDefinition
    {
        public final String name;
        public final String description;
        public final String parametersJson;

        public AIToolDefinition(String name, String description, String parametersJson)
        {
            this.name = name;
            this.description = description;
            this.parametersJson = parametersJson;
        }
    }
}
