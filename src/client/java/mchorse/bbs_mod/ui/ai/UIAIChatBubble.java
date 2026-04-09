package mchorse.bbs_mod.ui.ai;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.List;

/**
 * A single chat message bubble in the AI chat panel.
 * Renders with different colors based on the role (user, assistant, system, tool).
 */
public class UIAIChatBubble extends UIElement
{
    private final String role;
    private final String text;
    private final boolean success;
    private List<String> lines;
    private int lastWidth = -1;

    /* Colors */
    private static final int USER_BG = 0x80335599;
    private static final int ASSISTANT_BG = 0x80444444;
    private static final int SYSTEM_BG = 0x80666633;
    private static final int TOOL_SUCCESS_BG = 0x80336633;
    private static final int TOOL_ERROR_BG = 0x80663333;

    public UIAIChatBubble(String role, String text, boolean success)
    {
        super();
        this.role = role;
        this.text = text;
        this.success = success;
    }

    @Override
    public void render(UIContext context)
    {
        int bg = this.getBackgroundColor();
        int textColor = Colors.WHITE;
        FontRenderer font = context.batcher.getFont();
        String prefix = this.getRolePrefix();
        int contentWidth = Math.max(40, this.area.w - 12);

        if (this.lines == null || this.lastWidth != contentWidth)
        {
            this.lines = new ArrayList<>();
            this.lastWidth = contentWidth;

            if (!prefix.isEmpty())
            {
                this.lines.add(prefix);
            }

            this.lines.addAll(font.wrap(this.text, contentWidth));
            this.h(this.lines.size() * 12 + 10);

            if (this.getParentContainer() != null)
            {
                this.getParentContainer().resize();
            }
        }

        this.area.render(context.batcher, bg);

        int x = this.area.x + 6;
        int y = this.area.y + 6;
        int prefixColor = this.getPrefixColor();

        for (int i = 0; i < this.lines.size(); i++)
        {
            int color = i == 0 && !prefix.isEmpty() ? prefixColor : textColor;

            if (i == 1 && !prefix.isEmpty())
            {
                color = textColor;
            }

            context.batcher.textShadow(this.lines.get(i), x, y + i * 12, color);
        }

        super.render(context);
    }

    private int getBackgroundColor()
    {
        switch (this.role)
        {
            case "user": return USER_BG;
            case "assistant": return ASSISTANT_BG;
            case "tool": return this.success ? TOOL_SUCCESS_BG : TOOL_ERROR_BG;
            default: return SYSTEM_BG;
        }
    }

    private String getRolePrefix()
    {
        switch (this.role)
        {
            case "user": return "You:";
            case "assistant": return "AI:";
            case "tool": return "";
            default: return "System:";
        }
    }

    private int getPrefixColor()
    {
        switch (this.role)
        {
            case "user": return 0xFF88BBFF;
            case "assistant": return 0xFF88FF88;
            case "tool": return this.success ? 0xFF88FF88 : 0xFFFF8888;
            default: return 0xFFFFFF88;
        }
    }
}
