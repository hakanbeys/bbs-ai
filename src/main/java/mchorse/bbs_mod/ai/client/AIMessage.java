package mchorse.bbs_mod.ai.client;

import java.util.Base64;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AIMessage
{
    public enum Role
    {
        SYSTEM, USER, ASSISTANT, TOOL
    }

    private final Role role;
    private final String content;
    private final String toolCallId;
    private final String toolName;
    private final List<AIToolCall> assistantToolCalls;

    /* For image attachments */
    private String imageBase64;
    private String imageMimeType;

    public AIMessage(Role role, String content)
    {
        this(role, content, null, null, List.of());
    }

    public AIMessage(Role role, String content, String toolCallId, String toolName)
    {
        this(role, content, toolCallId, toolName, List.of());
    }

    public AIMessage(Role role, String content, String toolCallId, String toolName, List<AIToolCall> assistantToolCalls)
    {
        this.role = role;
        this.content = content;
        this.toolCallId = toolCallId;
        this.toolName = toolName;
        this.assistantToolCalls = assistantToolCalls == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(assistantToolCalls));
    }

    public static AIMessage system(String content)
    {
        return new AIMessage(Role.SYSTEM, content);
    }

    public static AIMessage user(String content)
    {
        return new AIMessage(Role.USER, content);
    }

    public static AIMessage assistant(String content)
    {
        return new AIMessage(Role.ASSISTANT, content);
    }

    public static AIMessage assistantToolCalls(String content, List<AIToolCall> toolCalls)
    {
        return new AIMessage(Role.ASSISTANT, content, null, null, toolCalls);
    }

    public static AIMessage toolResult(String toolCallId, String toolName, String content)
    {
        return new AIMessage(Role.TOOL, content, toolCallId, toolName);
    }

    public Role getRole()
    {
        return this.role;
    }

    public String getContent()
    {
        return this.content;
    }

    public String getToolCallId()
    {
        return this.toolCallId;
    }

    public String getToolName()
    {
        return this.toolName;
    }

    public boolean hasAssistantToolCalls()
    {
        return !this.assistantToolCalls.isEmpty();
    }

    public List<AIToolCall> getAssistantToolCalls()
    {
        return this.assistantToolCalls;
    }

    public boolean hasImage()
    {
        return this.imageBase64 != null;
    }

    public String getImageBase64()
    {
        return this.imageBase64;
    }

    public String getImageMimeType()
    {
        return this.imageMimeType;
    }

    public AIMessage withImage(byte[] imageData, String mimeType)
    {
        this.imageBase64 = Base64.getEncoder().encodeToString(imageData);
        this.imageMimeType = mimeType;
        return this;
    }
}
