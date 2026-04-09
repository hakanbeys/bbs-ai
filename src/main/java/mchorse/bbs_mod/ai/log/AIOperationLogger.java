package mchorse.bbs_mod.ai.log;

import mchorse.bbs_mod.BBSMod;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

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

    private static File getTraceDir()
    {
        File dir = new File(BBSMod.getSettingsFolder(), "ai/traces");
        dir.mkdirs();
        return dir;
    }

    public static File createTraceFile(String filmId, String runId)
    {
        String safeName = filmId.replaceAll("[^a-zA-Z0-9_-]", "_");
        String suffix = (runId == null || runId.isEmpty() ? UUID.randomUUID().toString() : runId).replaceAll("[^a-zA-Z0-9_-]", "_");
        return new File(getTraceDir(), safeName + "_" + suffix + ".trace.log");
    }

    public static void appendTrace(File traceFile, String kind, String content)
    {
        if (traceFile == null)
        {
            return;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(traceFile, true)))
        {
            String timestamp = LocalDateTime.now().format(FORMATTER);
            writer.println("[" + timestamp + "] " + kind + ": " + safeTruncate(content, 5000));
            writer.println();
        }
        catch (Exception e)
        {}
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
                writer.println("  INPUTS: " + safeTruncate(inputs, 500));
                writer.println("  RESULT: " + safeTruncate(result, 500));
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
                writer.println("[" + timestamp + "] " + role.toUpperCase() + ": " + safeTruncate(content, 1000));
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

    public static String safeTruncate(String text, int maxLength)
    {
        if (text == null) return "null";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
