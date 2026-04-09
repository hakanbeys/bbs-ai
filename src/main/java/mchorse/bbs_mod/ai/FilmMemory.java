package mchorse.bbs_mod.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.BBSMod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Per-film memory storage. Stores key facts about the film
 * that persist across AI sessions, saved to config directory.
 */
public class FilmMemory
{
    private final String filmId;
    private final List<String> memories = new ArrayList<>();
    private final File memoryFile;

    public FilmMemory(String filmId)
    {
        this.filmId = filmId;
        File dir = new File(BBSMod.getSettingsFolder(), "ai/memories");
        dir.mkdirs();
        this.memoryFile = new File(dir, sanitizeFilename(filmId) + ".json");
        this.load();
    }

    public String getFilmId()
    {
        return this.filmId;
    }

    public List<String> getMemories()
    {
        return this.memories;
    }

    public void addMemory(String memory)
    {
        this.memories.add(memory);
        this.save();
    }

    public void removeMemory(int index)
    {
        if (index >= 0 && index < this.memories.size())
        {
            this.memories.remove(index);
            this.save();
        }
    }

    public void clear()
    {
        this.memories.clear();
        this.save();
    }

    /**
     * Build a memory context string for inclusion in system prompt.
     */
    public String toContextString()
    {
        if (this.memories.isEmpty())
        {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n[Film Memories]\n");
        for (int i = 0; i < this.memories.size(); i++)
        {
            sb.append("- ").append(this.memories.get(i)).append("\n");
        }

        return sb.toString();
    }

    private void save()
    {
        try
        {
            JsonObject root = new JsonObject();
            root.addProperty("filmId", this.filmId);
            JsonArray arr = new JsonArray();
            for (String m : this.memories)
            {
                arr.add(m);
            }
            root.add("memories", arr);

            try (FileWriter writer = new FileWriter(this.memoryFile))
            {
                writer.write(root.toString());
            }
        }
        catch (Exception e)
        {
            /* Silently fail — memory is non-critical */
        }
    }

    private void load()
    {
        if (!this.memoryFile.exists()) return;

        try (FileReader reader = new FileReader(this.memoryFile))
        {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            if (root.has("memories"))
            {
                JsonArray arr = root.getAsJsonArray("memories");
                this.memories.clear();
                for (JsonElement elem : arr)
                {
                    this.memories.add(elem.getAsString());
                }
            }
        }
        catch (Exception e)
        {
            /* Silently fail */
        }
    }

    private static String sanitizeFilename(String name)
    {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
