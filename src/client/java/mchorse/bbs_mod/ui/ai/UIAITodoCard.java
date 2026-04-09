package mchorse.bbs_mod.ui.ai;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.framework.elements.utils.UIText;
import mchorse.bbs_mod.utils.colors.Colors;

public class UIAITodoCard extends UIElement
{
    private static final int CARD_BG = 0xA0181818;

    public UIAITodoCard(UIAIMessageParser.TodoCardData data)
    {
        super();
        this.markContainer();
        this.column(3).vertical().stretch();

        UILabel title = new UILabel(IKey.raw(data.title));
        title.w(1F).h(16);
        title.color(0xFFB6FFB6, true);
        this.add(title);

        for (UIAIMessageParser.TodoItem item : data.items)
        {
            UIText text = new UIText(getPrefix(item) + item.text);
            text.w(1F);
            text.padding(0, 1);
            text.color(getColor(item), true);
            this.add(text);
        }
    }

    private String getPrefix(UIAIMessageParser.TodoItem item)
    {
        switch (item.state)
        {
            case DONE: return "[x] ";
            case ACTIVE: return "[>] ";
            default: return "[ ] ";
        }
    }

    private int getColor(UIAIMessageParser.TodoItem item)
    {
        switch (item.state)
        {
            case DONE: return 0xFF9FE39F;
            case ACTIVE: return 0xFFFFD37A;
            default: return Colors.LIGHTEST_GRAY;
        }
    }

    @Override
    public void resize()
    {
        super.resize();
        this.h(this.calculateContentHeight() + 8);
    }

    private int calculateContentHeight()
    {
        int max = 0;

        for (IUIElement child : this.getChildren())
        {
            if (child instanceof UIElement)
            {
                UIElement element = (UIElement) child;
                max = Math.max(max, element.area.y - this.area.y + element.area.h);
            }
        }

        return max;
    }

    @Override
    public void render(UIContext context)
    {
        this.area.render(context.batcher, CARD_BG);
        super.render(context);
    }
}
