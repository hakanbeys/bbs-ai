package mchorse.bbs_mod.ai.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

public class MotionPresetTool implements IAITool
{
    @Override
    public String getName()
    {
        return "motion_preset";
    }

    @Override
    public String getDescription()
    {
        return "Apply a smooth animation preset to an actor. Best for reliable walk, run, idle, jump, and dramatic entrance blocking without hand-authoring every keyframe.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{" +
            "\"actor_index\":{\"type\":\"integer\",\"description\":\"Actor index from actor_list\"}," +
            "\"preset\":{\"type\":\"string\",\"enum\":[\"walk\",\"run\",\"idle\",\"jump\",\"dramatic_entrance\"],\"description\":\"Preset animation to apply\"}," +
            "\"start_tick\":{\"type\":\"integer\",\"description\":\"Tick to start from\"}," +
            "\"distance\":{\"type\":\"number\",\"description\":\"Travel distance for moving presets\"}," +
            "\"height\":{\"type\":\"number\",\"description\":\"Jump height or vertical accent\"}," +
            "\"axis\":{\"type\":\"string\",\"enum\":[\"x\",\"z\"],\"description\":\"Primary movement axis for moving presets\"}," +
            "\"loop\":{\"type\":\"boolean\",\"description\":\"Whether to set replay looping to the preset duration when appropriate\"}," +
            "\"body_yaw_amount\":{\"type\":\"number\",\"description\":\"Optional body sway intensity\"}" +
            "},\"required\":[\"actor_index\",\"preset\"]}";
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
            int actorIndex = args.get("actor_index").getAsInt();
            String preset = args.get("preset").getAsString();
            int startTick = args.has("start_tick") ? args.get("start_tick").getAsInt() : 0;
            double distance = args.has("distance") ? args.get("distance").getAsDouble() : defaultDistance(preset);
            double height = args.has("height") ? args.get("height").getAsDouble() : defaultHeight(preset);
            String axis = args.has("axis") ? args.get("axis").getAsString() : "z";
            boolean loop = !args.has("loop") || args.get("loop").getAsBoolean();
            double bodyYawAmount = args.has("body_yaw_amount") ? args.get("body_yaw_amount").getAsDouble() : defaultBodyYaw(preset);

            Replay replay = AIToolUtils.getReplay(film, actorIndex);

            if (replay == null)
            {
                return AIToolResult.error(AIToolUtils.replayOutOfRangeMessage(film, actorIndex));
            }

            final int[] changes = {0};
            final int[] duration = {presetDuration(preset)};

            BaseValue.edit(film, (f) ->
            {
                double baseX = sample(replay.keyframes.x, startTick);
                double baseY = sample(replay.keyframes.y, startTick);
                double baseZ = sample(replay.keyframes.z, startTick);
                double baseYaw = sample(replay.keyframes.yaw, startTick);
                double basePitch = sample(replay.keyframes.pitch, startTick);

                if ("walk".equals(preset))
                {
                    changes[0] += applyWalk(replay, startTick, duration[0], distance, axis, baseX, baseY, baseZ, baseYaw, bodyYawAmount, loop);
                }
                else if ("run".equals(preset))
                {
                    changes[0] += applyRun(replay, startTick, duration[0], distance, axis, baseX, baseY, baseZ, baseYaw, bodyYawAmount, loop);
                }
                else if ("idle".equals(preset))
                {
                    changes[0] += applyIdle(replay, startTick, duration[0], baseX, baseY, baseZ, baseYaw, basePitch, loop);
                }
                else if ("jump".equals(preset))
                {
                    changes[0] += applyJump(replay, startTick, duration[0], height, baseX, baseY, baseZ, baseYaw);
                }
                else if ("dramatic_entrance".equals(preset))
                {
                    changes[0] += applyEntrance(replay, startTick, duration[0], distance, axis, baseX, baseY, baseZ, baseYaw, bodyYawAmount);
                }
                else
                {
                    throw new IllegalArgumentException("Unknown preset: " + preset);
                }
            });

            return AIToolResult.success("Applied motion preset \"" + preset + "\" to actor \"" + replay.getName() + "\" for " + duration[0] + " ticks.", changes[0]);
        }
        catch (Exception e)
        {
            return AIToolResult.error("Motion preset failed: " + e.getMessage());
        }
    }

    private int applyWalk(Replay replay, int startTick, int duration, double distance, String axis, double baseX, double baseY, double baseZ, double baseYaw, double bodyYawAmount, boolean loop)
    {
        int changes = 0;
        int[] ticks = {0, 5, 10, 15, 20, 25, 30, 35, 40};
        double[] yOffsets = {0.0, -0.05, 0.03, 0.0, 0.0, -0.05, 0.03, 0.0, 0.0};
        double[] bodyYaw = {0, -0.5, -1.0, -0.5, 0, 0.5, 1.0, 0.5, 0};
        double[] headYaw = {0, -0.3, -0.5, -0.2, 0, 0.3, 0.5, 0.2, 0};

        for (int i = 0; i < ticks.length; i++)
        {
            double progress = (distance / 40D) * ticks[i];
            changes += insertPosition(replay, startTick + ticks[i], axis, baseX, baseY + yOffsets[i], baseZ, progress);
            changes += insert(replay.keyframes.yaw, startTick + ticks[i], baseYaw, "sine_in_out");
            changes += insert(replay.keyframes.bodyYaw, startTick + ticks[i], baseYaw + bodyYaw[i] * bodyYawAmount * 4D, "sine_in_out");
            changes += insert(replay.keyframes.headYaw, startTick + ticks[i], headYaw[i] * 3D, "sine_in_out");
        }

        if (loop)
        {
            replay.looping.set(duration);
            changes += 1;
        }

        return changes;
    }

    private int applyRun(Replay replay, int startTick, int duration, double distance, String axis, double baseX, double baseY, double baseZ, double baseYaw, double bodyYawAmount, boolean loop)
    {
        int changes = 0;
        int[] ticks = {0, 3, 5, 7, 10, 13, 15, 17, 20};
        double[] yOffsets = {0.0, 0.08, 0.12, 0.08, 0.0, 0.08, 0.12, 0.08, 0.0};
        double[] bodyYaw = {0, -0.8, -1.0, -0.5, 0, 0.8, 1.0, 0.5, 0};

        for (int i = 0; i < ticks.length; i++)
        {
            double progress = (distance / 20D) * ticks[i];
            changes += insertPosition(replay, startTick + ticks[i], axis, baseX, baseY + yOffsets[i], baseZ, progress);
            changes += insert(replay.keyframes.yaw, startTick + ticks[i], baseYaw, "cubic_in_out");
            changes += insert(replay.keyframes.bodyYaw, startTick + ticks[i], baseYaw + bodyYaw[i] * bodyYawAmount * 8D, "cubic_in_out");
            changes += insert(replay.keyframes.sprinting, startTick + ticks[i], 1D, "linear");
        }

        if (loop)
        {
            replay.looping.set(duration);
            changes += 1;
        }

        return changes;
    }

    private int applyIdle(Replay replay, int startTick, int duration, double baseX, double baseY, double baseZ, double baseYaw, double basePitch, boolean loop)
    {
        int changes = 0;
        int[] ticks = {0, 15, 30, 45, 60};
        double[] yOffsets = {0.0, 0.015, 0.02, 0.015, 0.0};
        double[] headYaw = {0.0, 1.5, 0.0, -1.5, 0.0};
        double[] pitch = {0.0, -0.5, 0.0, 0.5, 0.0};

        for (int i = 0; i < ticks.length; i++)
        {
            changes += insert(replay.keyframes.x, startTick + ticks[i], baseX, "sine_in_out");
            changes += insert(replay.keyframes.y, startTick + ticks[i], baseY + yOffsets[i], "sine_in_out");
            changes += insert(replay.keyframes.z, startTick + ticks[i], baseZ, "sine_in_out");
            changes += insert(replay.keyframes.yaw, startTick + ticks[i], baseYaw, "sine_in_out");
            changes += insert(replay.keyframes.pitch, startTick + ticks[i], basePitch + pitch[i], "sine_in_out");
            changes += insert(replay.keyframes.headYaw, startTick + ticks[i], headYaw[i], "sine_in_out");
        }

        if (loop)
        {
            replay.looping.set(duration);
            changes += 1;
        }

        return changes;
    }

    private int applyJump(Replay replay, int startTick, int duration, double height, double baseX, double baseY, double baseZ, double baseYaw)
    {
        int changes = 0;
        int[] ticks = {0, 4, 6, 10, 14, 18, 20, 24};
        double[] yOffsets = {0.0, -0.1, height * 0.35, height, height * 0.85, height * 0.2, -0.1, 0.0};
        double[] pitch = {0, 5, -5, -10, -5, 3, 8, 0};

        for (int i = 0; i < ticks.length; i++)
        {
            changes += insert(replay.keyframes.x, startTick + ticks[i], baseX, "quad_in_out");
            changes += insert(replay.keyframes.y, startTick + ticks[i], baseY + yOffsets[i], "quad_in_out");
            changes += insert(replay.keyframes.z, startTick + ticks[i], baseZ, "quad_in_out");
            changes += insert(replay.keyframes.yaw, startTick + ticks[i], baseYaw, "quad_in_out");
            changes += insert(replay.keyframes.pitch, startTick + ticks[i], pitch[i], "quad_in_out");
            changes += insert(replay.keyframes.grounded, startTick + ticks[i], (ticks[i] >= 20 || ticks[i] == 0) ? 1D : 0D, "step");
        }

        return changes;
    }

    private int applyEntrance(Replay replay, int startTick, int duration, double distance, String axis, double baseX, double baseY, double baseZ, double baseYaw, double bodyYawAmount)
    {
        int changes = 0;
        int[] ticks = {0, 20, 40, 50, 55, 60};
        double[] progress = {-distance, -distance * 0.5, -distance * 0.1, -distance * 0.02, 0.0, 0.0};
        double[] yaw = {baseYaw + 15, baseYaw + 10, baseYaw + 5, baseYaw + 2, baseYaw, baseYaw};
        double[] headYaw = {0, -2, -1, 0, 0, 0};
        double[] pitch = {0, 0, 0, 0, 0, -3};

        for (int i = 0; i < ticks.length; i++)
        {
            changes += insertPosition(replay, startTick + ticks[i], axis, baseX, baseY, baseZ, progress[i]);
            changes += insert(replay.keyframes.yaw, startTick + ticks[i], yaw[i], "cubic_in_out");
            changes += insert(replay.keyframes.bodyYaw, startTick + ticks[i], yaw[i] - (bodyYawAmount * 3D), "cubic_in_out");
            changes += insert(replay.keyframes.headYaw, startTick + ticks[i], headYaw[i], "sine_in_out");
            changes += insert(replay.keyframes.pitch, startTick + ticks[i], pitch[i], "sine_in_out");
        }

        return changes;
    }

    private int insertPosition(Replay replay, int tick, String axis, double baseX, double y, double baseZ, double progress)
    {
        if ("x".equalsIgnoreCase(axis))
        {
            return insert(replay.keyframes.x, tick, baseX + progress, "cubic_in_out")
                + insert(replay.keyframes.y, tick, y, "cubic_in_out")
                + insert(replay.keyframes.z, tick, baseZ, "cubic_in_out");
        }

        return insert(replay.keyframes.x, tick, baseX, "cubic_in_out")
            + insert(replay.keyframes.y, tick, y, "cubic_in_out")
            + insert(replay.keyframes.z, tick, baseZ + progress, "cubic_in_out");
    }

    private int insert(KeyframeChannel<Double> channel, int tick, double value, String interpolation)
    {
        int index = channel.insert(tick, value);

        return 1 + AIToolUtils.applyInterpolation(channel, index, interpolation);
    }

    private double sample(KeyframeChannel<Double> channel, int tick)
    {
        return channel.isEmpty() ? 0D : channel.interpolate(tick);
    }

    private int presetDuration(String preset)
    {
        if ("run".equals(preset))
        {
            return 20;
        }
        if ("idle".equals(preset))
        {
            return 60;
        }
        if ("jump".equals(preset))
        {
            return 24;
        }
        if ("dramatic_entrance".equals(preset))
        {
            return 60;
        }

        return 40;
    }

    private double defaultDistance(String preset)
    {
        if ("run".equals(preset))
        {
            return 3D;
        }
        if ("dramatic_entrance".equals(preset))
        {
            return 8D;
        }

        return 2D;
    }

    private double defaultHeight(String preset)
    {
        return "jump".equals(preset) ? 1.2D : 0.8D;
    }

    private double defaultBodyYaw(String preset)
    {
        if ("run".equals(preset))
        {
            return 1.2D;
        }

        return 1D;
    }
}
