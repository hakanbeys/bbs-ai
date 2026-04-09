package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.camera.clips.overwrite.KeyframeClip;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

public class CameraPresetTool implements IAITool
{
    @Override
    public String getName()
    {
        return "camera_preset";
    }

    @Override
    public String getDescription()
    {
        return "Create a cinematic camera preset around an actor or focus point. Best for reliable establishing, close-up, orbit, low-angle, and tracking shots.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"preset\":{\"type\":\"string\",\"enum\":[\"establishing_wide\",\"close_up\",\"orbit\",\"low_angle\",\"tracking\"],\"description\":\"Camera preset\"}," +
            "\"focus_actor_index\":{\"type\":\"integer\",\"description\":\"Optional focus actor index\"}," +
            "\"focus_x\":{\"type\":\"number\"},\"focus_y\":{\"type\":\"number\"},\"focus_z\":{\"type\":\"number\"}," +
            "\"tick\":{\"type\":\"integer\"},\"duration\":{\"type\":\"integer\"},\"title\":{\"type\":\"string\"}," +
            "\"distance\":{\"type\":\"number\",\"description\":\"Camera radius or offset distance\"}," +
            "\"height_offset\":{\"type\":\"number\",\"description\":\"Height offset from the focus point\"}" +
            "},\"required\":[\"preset\"]}";
    }

    @Override
    public AIToolResult execute(String argumentsJson, AIToolContext context)
    {
        Film film = context.getFilm();

        if (film == null)
        {
            return AIToolResult.error("No film is currently open.");
        }

        try
        {
            JsonObject args = JsonParser.parseString(argumentsJson).getAsJsonObject();
            String preset = args.get("preset").getAsString();
            int tick = args.has("tick") ? args.get("tick").getAsInt() : 0;
            int duration = args.has("duration") ? args.get("duration").getAsInt() : defaultDuration(preset);
            String title = args.has("title") ? args.get("title").getAsString() : preset;
            double distance = args.has("distance") ? args.get("distance").getAsDouble() : defaultDistance(preset);
            double heightOffset = args.has("height_offset") ? args.get("height_offset").getAsDouble() : defaultHeightOffset(preset);

            Focus focus = resolveFocus(film, args, tick);

            if (focus == null)
            {
                return AIToolResult.error("Could not resolve focus point. Provide focus_actor_index or focus_x/focus_y/focus_z.");
            }

            BaseValue.edit(film, (f) ->
            {
                KeyframeClip clip = new KeyframeClip();
                clip.title.set(title);
                clip.tick.set(tick);
                clip.duration.set(duration);

                if ("establishing_wide".equals(preset))
                {
                    addEstablishing(clip, focus, distance, heightOffset, duration);
                }
                else if ("close_up".equals(preset))
                {
                    addCloseUp(clip, focus, distance, heightOffset, duration);
                }
                else if ("orbit".equals(preset))
                {
                    addOrbit(clip, focus, distance, heightOffset, duration);
                }
                else if ("low_angle".equals(preset))
                {
                    addLowAngle(clip, focus, distance, heightOffset, duration);
                }
                else if ("tracking".equals(preset))
                {
                    addTracking(clip, focus, distance, heightOffset, duration);
                }
                else
                {
                    throw new IllegalArgumentException("Unknown preset: " + preset);
                }

                f.camera.addClip(clip);
            });

            return AIToolResult.success("Added camera preset \"" + preset + "\" around focus \"" + focus.label + "\".", 1);
        }
        catch (Exception e)
        {
            return AIToolResult.error("Camera preset failed: " + e.getMessage());
        }
    }

    private void addEstablishing(KeyframeClip clip, Focus focus, double distance, double heightOffset, int duration)
    {
        addFrame(clip, 0, focus.x + distance, focus.y + heightOffset + 2.5D, focus.z - distance * 0.7D, -35D, 15D, 0D, 40D, "cubic_in_out");
        addFrame(clip, duration, focus.x + distance * 0.75D, focus.y + heightOffset + 1.7D, focus.z - distance * 0.45D, -28D, 12D, 0D, 45D, "cubic_in_out");
    }

    private void addCloseUp(KeyframeClip clip, Focus focus, double distance, double heightOffset, int duration)
    {
        addFrame(clip, 0, focus.x + distance, focus.y + heightOffset + 0.8D, focus.z, 90D, 5D, 0D, 50D, "sine_in_out");
        addFrame(clip, duration, focus.x + distance * 0.8D, focus.y + heightOffset + 0.85D, focus.z + 0.2D, 85D, 3D, 0D, 48D, "sine_in_out");
    }

    private void addOrbit(KeyframeClip clip, Focus focus, double distance, double heightOffset, int duration)
    {
        addFrame(clip, 0, focus.x + distance, focus.y + heightOffset + 0.8D, focus.z, 90D, 6D, 0D, 55D, "sine_in_out");
        addFrame(clip, duration / 4, focus.x, focus.y + heightOffset + 0.95D, focus.z + distance, 180D, 5D, 0D, 54D, "sine_in_out");
        addFrame(clip, duration / 2, focus.x - distance, focus.y + heightOffset + 0.8D, focus.z, 270D, 6D, 0D, 55D, "sine_in_out");
        addFrame(clip, (duration * 3) / 4, focus.x, focus.y + heightOffset + 0.95D, focus.z - distance, 0D, 5D, 0D, 54D, "sine_in_out");
        addFrame(clip, duration, focus.x + distance, focus.y + heightOffset + 0.8D, focus.z, 90D, 6D, 0D, 55D, "sine_in_out");
    }

    private void addLowAngle(KeyframeClip clip, Focus focus, double distance, double heightOffset, int duration)
    {
        addFrame(clip, 0, focus.x + distance * 0.75D, focus.y - 0.4D, focus.z + distance * 0.15D, 105D, -24D, 0D, 62D, "cubic_in_out");
        addFrame(clip, duration, focus.x + distance * 0.55D, focus.y - 0.2D, focus.z, 95D, -18D, 0D, 60D, "cubic_in_out");
    }

    private void addTracking(KeyframeClip clip, Focus focus, double distance, double heightOffset, int duration)
    {
        addFrame(clip, 0, focus.x + distance, focus.y + heightOffset + 0.7D, focus.z - 1.2D, 80D, 4D, 0D, 58D, "cubic_in_out");
        addFrame(clip, duration, focus.x + distance, focus.y + heightOffset + 0.7D, focus.z + 1.8D, 80D, 4D, 0D, 58D, "cubic_in_out");
    }

    private void addFrame(KeyframeClip clip, int tick, double x, double y, double z, double yaw, double pitch, double roll, double fov, String interpolation)
    {
        insert(clip.x, tick, x, interpolation);
        insert(clip.y, tick, y, interpolation);
        insert(clip.z, tick, z, interpolation);
        insert(clip.yaw, tick, yaw, interpolation);
        insert(clip.pitch, tick, pitch, interpolation);
        insert(clip.roll, tick, roll, interpolation);
        insert(clip.fov, tick, fov, interpolation);
    }

    private void insert(KeyframeChannel<Double> channel, int tick, double value, String interpolation)
    {
        int index = channel.insert(tick, value);
        AIToolUtils.applyInterpolation(channel, index, interpolation);
    }

    private Focus resolveFocus(Film film, JsonObject args, int tick)
    {
        if (args.has("focus_actor_index"))
        {
            int actorIndex = args.get("focus_actor_index").getAsInt();
            Replay replay = AIToolUtils.getReplay(film, actorIndex);

            if (replay == null)
            {
                throw new IllegalArgumentException(AIToolUtils.replayOutOfRangeMessage(film, actorIndex));
            }

            return new Focus(
                replay.getName(),
                sample(replay.keyframes.x, tick),
                sample(replay.keyframes.y, tick),
                sample(replay.keyframes.z, tick)
            );
        }

        if (args.has("focus_x") && args.has("focus_y") && args.has("focus_z"))
        {
            return new Focus(
                "point",
                args.get("focus_x").getAsDouble(),
                args.get("focus_y").getAsDouble(),
                args.get("focus_z").getAsDouble()
            );
        }

        return null;
    }

    private double sample(KeyframeChannel<Double> channel, int tick)
    {
        return channel.isEmpty() ? 0D : channel.interpolate(tick);
    }

    private int defaultDuration(String preset)
    {
        if ("orbit".equals(preset))
        {
            return 60;
        }

        return 40;
    }

    private double defaultDistance(String preset)
    {
        if ("establishing_wide".equals(preset))
        {
            return 10D;
        }
        if ("close_up".equals(preset))
        {
            return 1.5D;
        }
        if ("orbit".equals(preset))
        {
            return 4D;
        }
        if ("low_angle".equals(preset))
        {
            return 2D;
        }

        return 2.5D;
    }

    private double defaultHeightOffset(String preset)
    {
        if ("establishing_wide".equals(preset))
        {
            return 2D;
        }
        if ("close_up".equals(preset))
        {
            return 1.1D;
        }

        return 1.2D;
    }

    private static class Focus
    {
        public final String label;
        public final double x;
        public final double y;
        public final double z;

        public Focus(String label, double x, double y, double z)
        {
            this.label = label;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
