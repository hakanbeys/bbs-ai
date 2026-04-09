package mchorse.bbs_mod.ai.session;

import mchorse.bbs_mod.ai.client.AIMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single AI chat session for a film.
 * A film can have multiple sessions (tabs).
 */
public class AISession
{
    private final String id;
    private String name;
    private final List<AIMessage> messages = new ArrayList<>();
    private boolean active;

    public AISession(String id, String name)
    {
        this.id = id;
        this.name = name;
        this.active = false;
    }

    public String getId()
    {
        return this.id;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public List<AIMessage> getMessages()
    {
        return this.messages;
    }

    public void addMessage(AIMessage message)
    {
        this.messages.add(message);
    }

    public void clearMessages()
    {
        this.messages.clear();
    }

    public boolean isActive()
    {
        return this.active;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

    public boolean isEmpty()
    {
        return this.messages.isEmpty();
    }
}
