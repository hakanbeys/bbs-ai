package mchorse.bbs_mod.ai.tools;

import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.utils.undo.UndoManager;

/**
 * Context passed to AI tools when executing.
 * Holds references to the current film and undo system.
 */
public class AIToolContext
{
    private final Film film;
    private final String filmId;
    private final UndoManager undoManager;

    /* Preview request state */
    private boolean previewRequested;
    private int previewFromTick;
    private int previewToTick = -1;

    public AIToolContext(Film film, String filmId, UndoManager undoManager)
    {
        this.film = film;
        this.filmId = filmId;
        this.undoManager = undoManager;
    }

    public Film getFilm()
    {
        return this.film;
    }

    public String getFilmId()
    {
        return this.filmId;
    }

    public UndoManager getUndoManager()
    {
        return this.undoManager;
    }

    public void requestPreview(int fromTick, int toTick)
    {
        this.previewRequested = true;
        this.previewFromTick = fromTick;
        this.previewToTick = toTick;
    }

    public boolean isPreviewRequested()
    {
        return this.previewRequested;
    }

    public int getPreviewFromTick()
    {
        return this.previewFromTick;
    }

    public int getPreviewToTick()
    {
        return this.previewToTick;
    }
}
