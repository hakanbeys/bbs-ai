package mchorse.bbs_mod.ai.log;

import mchorse.bbs_mod.BBSMod;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AIOperationLogger
{
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static File getLogFile(String filmId)
    {
        File dir = new File(BBSMod.getSettingsFolder(), "ai/logs");
        dir.mkdirs();

        String safeName = filmId.replaceAll("[^a-zA-Z0-9_-]", "_");
        return new File(dir, safeName + "_ai.log");
    }

    public static void log(String filmId, String toolName, String inputs, String result)
    {
        try
        {
            File logFile = getLogFile(filmId);

            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true)))
            {
                String timestamp = LocalDateTime.now().format(FORMATTER);
                writer.println("[" + timestamp + "] TOOL: " + toolName);
                writer.println("  INPUTS: " + truncate(inputs, 500));
                writer.println("  RESULT: " + truncate(result, 500));
                writer.println();
            }
        }
        catch (Exception e)
        {
            /* Silent fail — logging should never crash the mod */
        }
    }

    public static void logMessage(String filmId, String role, String content)
    {
        try
        {
            File logFile = getLogFile(filmId);

            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true)))
            {
                String timestamp = LocalDateTime.now().format(FORMATTER);
                writer.println("[" + timestamp + "] " + role.toUpperCase() + ": " + truncate(content, 1000));
                writer.println();
            }
        }
        catch (Exception e)
        {
            /* Silent fail */
        }
    }

    public static void logError(String filmId, String error)
    {
        try
        {
            File logFile = getLogFile(filmId);

            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true)))
            {
                String timestamp = LocalDateTime.now().format(FORMATTER);
                writer.println("[" + timestamp + "] ERROR: " + error);
                writer.println();
            }
        }
        catch (Exception e)
        {
            /* Silent fail */
        }
    }

    private static String truncate(String text, int maxLength)
    {
        if (text == null) return "null";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
