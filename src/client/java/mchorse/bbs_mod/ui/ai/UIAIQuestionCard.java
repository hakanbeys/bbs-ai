package mchorse.bbs_mod.ui.ai;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.framework.elements.utils.UIText;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UIAIQuestionCard extends UIElement
{
    private static final int CARD_BG = 0xA0222222;

    private final List<UIElement> controls = new ArrayList<>();

    public UIAIQuestionCard(UIAIMessageParser.QuestionCardData data, Consumer<String> onSubmit)
    {
        super();
        this.markContainer();
        this.column(4).vertical().stretch();

        UILabel title = new UILabel(IKey.raw(data.title));
        title.w(1F).h(16);
        title.color(0xFFD7E6FF, true);
        this.add(title);

        if (!data.prompt.isEmpty())
        {
            UIText prompt = new UIText(data.prompt);
            prompt.w(1F);
            prompt.padding(0, 2);
            prompt.color(Colors.LIGHTEST_GRAY, true);
            this.add(prompt);
        }

        if (data.multi)
        {
            List<UIToggle> toggles = new ArrayList<>();

            for (String option : data.options)
            {
                UIToggle toggle = new UIToggle(IKey.raw(option), false, (b) -> {});
                toggle.w(1F).h(18);
                toggles.add(toggle);
                this.controls.add(toggle);
                this.add(toggle);
            }

            UIButton submit = new UIButton(IKey.raw(data.submitLabel), (b) ->
            {
                List<String> selected = new ArrayList<>();

                for (UIToggle toggle : toggles)
                {
                    if (toggle.getValue())
                    {
                        selected.add(toggle.label.get());
                    }
                }

                if (!selected.isEmpty())
                {
                    this.disableControls();
                    onSubmit.accept("Selections for \"" + data.title + "\": " + String.join(", ", selected));
                }
            });
            submit.w(1F).h(22);
            this.controls.add(submit);
            this.add(submit);
        }
        else
        {
            for (String option : data.options)
            {
                UIButton button = new UIButton(IKey.raw(option), (b) ->
                {
                    this.disableControls();
                    onSubmit.accept("Selection for \"" + data.title + "\": " + option);
                });
                button.w(1F).h(22);
                this.controls.add(button);
                this.add(button);
            }
        }
    }

    private void disableControls()
    {
        for (UIElement control : this.controls)
        {
            control.setEnabled(false);
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
