package mchorse.bbs_mod.ui.ai;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.List;

/**
 * A single chat message bubble in the AI chat panel.
 * Supports multi-line text wrapping. Height adjusts dynamically.
 */
public class UIAIChatBubble extends UIElement
{
    private final String role;
    private final String text;
    private final boolean success;

    /* Cached wrapped lines */
    private List<String> wrappedLines;
    private int lastWrapWidth = -1;

    /* Colors */
    private static final int USER_BG = 0x80335599;
    private static final int ASSISTANT_BG = 0x80444444;
    private static final int SYSTEM_BG = 0x80666633;
    private static final int TOOL_SUCCESS_BG = 0x80336633;
    private static final int TOOL_ERROR_BG = 0x80663333;

    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 5;

    public UIAIChatBubble(String role, String text, boolean success)
    {
        super();
        this.role = role;
        this.text = text != null ? text : "";
        this.success = success;
    }

    @Override
    public void render(UIContext context)
    {
        FontRenderer font = context.batcher.getFont();
        int contentWidth = this.area.w - PADDING * 2;

        /* Re-wrap if width changed */
        if (contentWidth != this.lastWrapWidth || this.wrappedLines == null)
        {
            this.lastWrapWidth = contentWidth;
            this.wrapText(font, contentWidth);

            /* Update height based on line count */
            int newHeight = Math.max(PADDING * 2 + this.wrappedLines.size() * LINE_HEIGHT, 20);
            if (newHeight != this.area.h)
            {
                this.h(newHeight);

                /* Re-layout parent */
                if (this.getParent() != null)
                {
                    this.getParent().resize();
                }
            }
        }

        int bg = this.getBackgroundColor();

        /* Render background */
        this.area.render(context.batcher, bg);

        /* Render prefix on first line */
        String prefix = this.getRolePrefix();
        int prefixColor = this.getPrefixColor();
        int x = this.area.x + PADDING;
        int y = this.area.y + PADDING;

        if (!prefix.isEmpty() && !this.wrappedLines.isEmpty())
        {
            context.batcher.textShadow(prefix, x, y, prefixColor);
            int prefixWidth = font.getWidth(prefix) + 4;

            /* First line after prefix */
            String firstLine = this.wrappedLines.get(0);
            context.batcher.textShadow(firstLine, x + prefixWidth, y, Colors.WHITE);
            y += LINE_HEIGHT;

            /* Remaining lines */
            for (int i = 1; i < this.wrappedLines.size(); i++)
            {
                context.batcher.textShadow(this.wrappedLines.get(i), x, y, Colors.WHITE);
                y += LINE_HEIGHT;
            }
        }
        else
        {
            for (String line : this.wrappedLines)
            {
                context.batcher.textShadow(line, x, y, Colors.WHITE);
                y += LINE_HEIGHT;
            }
        }

        super.render(context);
    }

    private void wrapText(FontRenderer font, int maxWidth)
    {
        String prefix = this.getRolePrefix();
        int prefixWidth = prefix.isEmpty() ? 0 : font.getWidth(prefix) + 4;

        this.wrappedLines = new ArrayList<>();

        /* Split by newlines first */
        String[] paragraphs = this.text.split("\n");

        for (int p = 0; p < paragraphs.length; p++)
        {
            String paragraph = paragraphs[p];
            int availWidth = (p == 0 && this.wrappedLines.isEmpty()) ? maxWidth - prefixWidth : maxWidth;

            if (availWidth <= 10) availWidth = maxWidth;

            if (paragraph.isEmpty())
            {
                this.wrappedLines.add("");
                continue;
            }

            /* Word wrap */
            List<String> lines = font.wrap(paragraph, availWidth);
            this.wrappedLines.addAll(lines);
        }

        if (this.wrappedLines.isEmpty())
        {
            this.wrappedLines.add("");
        }
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
