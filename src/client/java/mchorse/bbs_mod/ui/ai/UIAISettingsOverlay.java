package mchorse.bbs_mod.ui.ai;

import mchorse.bbs_mod.ai.AISettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;

/**
 * Settings overlay for AI configuration.
 * Allows selecting provider, model, API keys, and system prompt.
 */
public class UIAISettingsOverlay extends UIOverlayPanel
{
    private UITextbox providerInput;
    private UITextbox modelInput;
    private UITextbox apiKeyInput;
    private UITextbox baseUrlInput;
    private UITextbox webSearchProviderInput;
    private UITextbox webSearchApiKeyInput;
    private UITextbox systemPromptInput;
    private UITextbox maxKeyframesInput;

    public UIAISettingsOverlay()
    {
        super(IKey.raw("AI Settings"));
        this.buildContent();
    }

    private void buildContent()
    {
        UIScrollView scroll = new UIScrollView();
        scroll.column(6).vertical().stretch();
        scroll.full(this.content);

        UIElement fields = new UIElement();
        fields.column(6).vertical().stretch();

        /* Provider */
        fields.add(this.label("Provider (anthropic/openai/google/openrouter)"));
        this.providerInput = new UITextbox((text) -> AISettings.aiProvider.set(text));
        this.providerInput.setText(AISettings.aiProvider.get());
        this.providerInput.h(20);
        fields.add(this.providerInput);

        /* Model */
        fields.add(this.label("Model"));
        this.modelInput = new UITextbox((text) -> AISettings.aiModel.set(text));
        this.modelInput.setText(AISettings.aiModel.get());
        this.modelInput.h(20);
        fields.add(this.modelInput);

        /* API Key */
        fields.add(this.label("API Key"));
        this.apiKeyInput = new UITextbox((text) -> AISettings.aiApiKey.set(text));
        this.apiKeyInput.setText(AISettings.aiApiKey.get());
        this.apiKeyInput.h(20);
        fields.add(this.apiKeyInput);

        /* Base URL */
        fields.add(this.label("Base URL (optional)"));
        this.baseUrlInput = new UITextbox((text) -> AISettings.aiBaseUrl.set(text));
        this.baseUrlInput.setText(AISettings.aiBaseUrl.get());
        this.baseUrlInput.h(20);
        fields.add(this.baseUrlInput);

        /* Web Search Provider */
        fields.add(this.label("Web Search Provider (tavily/brave)"));
        this.webSearchProviderInput = new UITextbox((text) -> AISettings.webSearchProvider.set(text));
        this.webSearchProviderInput.setText(AISettings.webSearchProvider.get());
        this.webSearchProviderInput.h(20);
        fields.add(this.webSearchProviderInput);

        /* Web Search API Key */
        fields.add(this.label("Web Search API Key"));
        this.webSearchApiKeyInput = new UITextbox((text) -> AISettings.webSearchApiKey.set(text));
        this.webSearchApiKeyInput.setText(AISettings.webSearchApiKey.get());
        this.webSearchApiKeyInput.h(20);
        fields.add(this.webSearchApiKeyInput);

        /* Max Keyframes */
        fields.add(this.label("Max Keyframes Per Operation"));
        this.maxKeyframesInput = new UITextbox((text) ->
        {
            try
            {
                int val = Integer.parseInt(text);
                if (val >= 1 && val <= 10000)
                {
                    AISettings.maxKeyframesPerOperation.set(val);
                }
            }
            catch (NumberFormatException e) {}
        });
        this.maxKeyframesInput.setText(String.valueOf(AISettings.maxKeyframesPerOperation.get()));
        this.maxKeyframesInput.h(20);
        fields.add(this.maxKeyframesInput);

        /* System Prompt */
        fields.add(this.label("System Prompt"));
        this.systemPromptInput = new UITextbox(10000, (text) -> AISettings.systemPrompt.set(text));
        this.systemPromptInput.setText(AISettings.systemPrompt.get());
        this.systemPromptInput.h(20);
        fields.add(this.systemPromptInput);

        /* Reset button */
        UIButton resetBtn = new UIButton(IKey.raw("Reset System Prompt"), (b) ->
        {
            AISettings.systemPrompt.set(AISettings.DEFAULT_SYSTEM_PROMPT);
            this.systemPromptInput.setText(AISettings.DEFAULT_SYSTEM_PROMPT);
        });
        resetBtn.h(20);
        fields.add(resetBtn);

        scroll.add(fields);
        this.content.add(scroll);
    }

    private UILabel label(String text)
    {
        UILabel lbl = new UILabel(IKey.raw(text));
        lbl.h(14);
        return lbl;
    }
}
