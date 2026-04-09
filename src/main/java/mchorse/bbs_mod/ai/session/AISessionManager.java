package mchorse.bbs_mod.ai.session;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages multiple AI chat sessions per film.
 * Each film gets its own set of sessions (tabs).
 */
public class AISessionManager
{
    private final Map<String, List<AISession>> filmSessions = new LinkedHashMap<>();
    private final Map<String, AISession> activeSession = new LinkedHashMap<>();

    public AISession getOrCreateSession(String filmId)
    {
        AISession active = activeSession.get(filmId);
        if (active != null)
        {
            return active;
        }

        return createSession(filmId, "Chat 1");
    }

    public AISession createSession(String filmId, String name)
    {
        List<AISession> sessions = filmSessions.computeIfAbsent(filmId, k -> new ArrayList<>());
        AISession session = new AISession(UUID.randomUUID().toString(), name);
        sessions.add(session);

        setActiveSession(filmId, session);
        return session;
    }

    public void setActiveSession(String filmId, AISession session)
    {
        /* Deactivate previous */
        AISession prev = activeSession.get(filmId);
        if (prev != null)
        {
            prev.setActive(false);
        }

        session.setActive(true);
        activeSession.put(filmId, session);
    }

    public AISession getActiveSession(String filmId)
    {
        return activeSession.get(filmId);
    }

    public List<AISession> getSessions(String filmId)
    {
        return filmSessions.computeIfAbsent(filmId, k -> new ArrayList<>());
    }

    public void removeSession(String filmId, AISession session)
    {
        List<AISession> sessions = filmSessions.get(filmId);
        if (sessions != null)
        {
            sessions.remove(session);

            if (activeSession.get(filmId) == session)
            {
                activeSession.remove(filmId);
                if (!sessions.isEmpty())
                {
                    setActiveSession(filmId, sessions.get(sessions.size() - 1));
                }
            }
        }
    }

    public void clearFilmSessions(String filmId)
    {
        filmSessions.remove(filmId);
        activeSession.remove(filmId);
    }
}
