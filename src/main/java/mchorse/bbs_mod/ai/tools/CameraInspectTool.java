package mchorse.bbs_mod.ai.tools;

import mchorse.bbs_mod.camera.clips.overwrite.KeyframeClip;
import mchorse.bbs_mod.camera.clips.overwrite.PathClip;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.utils.clips.Clip;

import java.util.List;

public class CameraInspectTool implements IAITool
{
    @Override
    public String getName()
    {
        return "camera_inspect";
    }

    @Override
    public String getDescription()
    {
        return "Inspect current camera clips, their types, timing, and basic keyframe/path counts.";
    }

    @Override
    public String getParametersSchema()
    {
        return "{\"type\":\"object\",\"properties\":{},\"required\":[]}";
    }

    @Override
    public AIToolResult execute(String argumentsJson, AIToolContext context)
    {
        Film film = context.getFilm();

        if (film == null)
        {
            return AIToolResult.error("No film is currently open.");
        }

        List<Clip> clips = film.camera.get();

        if (clips.isEmpty())
        {
            return AIToolResult.success("No camera clips in the film.");
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Camera clips (").append(clips.size()).append("):\n");

        for (int i = 0; i < clips.size(); i++)
        {
            Clip clip = clips.get(i);

            builder.append("- [").append(i).append("] ");
            builder.append(clip.title.get().isEmpty() ? clip.getClass().getSimpleName() : clip.title.get());
            builder.append(" | type=").append(clip.getClass().getSimpleName());
            builder.append(" | tick=").append(clip.tick.get());
            builder.append(" | duration=").append(clip.duration.get());

            if (clip instanceof KeyframeClip keyframe)
            {
                int keyframeCount = keyframe.x.getKeyframes().size() + keyframe.y.getKeyframes().size() + keyframe.z.getKeyframes().size() +
                    keyframe.yaw.getKeyframes().size() + keyframe.pitch.getKeyframes().size() + keyframe.roll.getKeyframes().size() + keyframe.fov.getKeyframes().size();
                builder.append(" | keyframes=").append(keyframeCount);
            }
            else if (clip instanceof PathClip path)
            {
                builder.append(" | points=").append(path.points.size());
            }

            builder.append("\n");
        }

        return AIToolResult.success(builder.toString());
    }
}
