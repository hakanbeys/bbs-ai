package mchorse.bbs_mod.ui.ai;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ai.client.AIModelFetcher;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;

import java.util.Arrays;
import java.util.List;

/**
 * Settings overlay for AI configuration.
 * Uses UICirculate dropdowns for provider/search provider.
 * Fetches model list from provider API.
 */
public class UIAISettingsOverlay extends UIOverlayPanel
{
    private static final String[] PROVIDERS = {"anthropic", "openai", "google", "openrouter"};
    private static final String[] WEB_SEARCH_PROVIDERS = {"tavily", "brave"};

    private UICirculate providerSelect;
    private UICirculate webSearchSelect;
    private UICirculate modelSelect;
    private UITextbox apiKeyInput;
    private UIButton fetchModelsButton;

    private List<String> fetchedModels;

    public UIAISettingsOverlay()
    {
        super(IKey.raw("AI Animator Settings"));
        this.buildContent();
    }

    private void buildContent()
    {
        UIScrollView scroll = new UIScrollView();
        scroll.column(5).vertical().stretch();
        scroll.full(this.content);

        UIElement fields = new UIElement();
        fields.column(5).vertical().stretch();

        /* Provider dropdown */
        fields.add(label("Provider"));
        this.providerSelect = new UICirculate((c) ->
        {
            BBSSettings.aiProvider.set(PROVIDERS[c.getValue()]);
            this.refreshModelList();
        });
        for (String p : PROVIDERS)
        {
            this.providerSelect.addLabel(IKey.raw(p));
        }
        int provIdx = Arrays.asList(PROVIDERS).indexOf(BBSSettings.aiProvider.get());
        this.providerSelect.setValue(Math.max(0, provIdx));
        this.providerSelect.h(20);
        fields.add(this.providerSelect);

        /* API Key */
        fields.add(label("API Key"));
        this.apiKeyInput = new UITextbox((text) -> BBSSettings.aiApiKey.set(text));
        this.apiKeyInput.setText(BBSSettings.aiApiKey.get());
        this.apiKeyInput.h(20);
        fields.add(this.apiKeyInput);

        /* Model dropdown + fetch */
        fields.add(label("Model"));

        UIElement modelRow = new UIElement();
        modelRow.row(4);

        this.modelSelect = new UICirculate((c) ->
        {
            if (this.fetchedModels != null && c.getValue() < this.fetchedModels.size())
            {
                BBSSettings.aiModel.set(this.fetchedModels.get(c.getValue()));
            }
        });
        this.modelSelect.addLabel(IKey.raw(BBSSettings.aiModel.get()));
        this.modelSelect.h(20);
        this.modelSelect.w(1F, -65);

        this.fetchModelsButton = new UIButton(IKey.raw("Fetch"), (b) -> this.fetchModels());
        this.fetchModelsButton.w(55).h(20);

        modelRow.add(this.modelSelect, this.fetchModelsButton);
        fields.add(modelRow);

        /* Base URL */
        fields.add(label("Base URL (optional)"));
        UITextbox baseUrl = new UITextbox((text) -> BBSSettings.aiBaseUrl.set(text));
        baseUrl.setText(BBSSettings.aiBaseUrl.get());
        baseUrl.h(20);
        fields.add(baseUrl);

        /* Web Search Provider dropdown */
        fields.add(label("Web Search Provider"));
        this.webSearchSelect = new UICirculate((c) ->
        {
            BBSSettings.aiWebSearchProvider.set(WEB_SEARCH_PROVIDERS[c.getValue()]);
        });
        for (String p : WEB_SEARCH_PROVIDERS)
        {
            this.webSearchSelect.addLabel(IKey.raw(p));
        }
        int wsIdx = Arrays.asList(WEB_SEARCH_PROVIDERS).indexOf(BBSSettings.aiWebSearchProvider.get());
        this.webSearchSelect.setValue(Math.max(0, wsIdx));
        this.webSearchSelect.h(20);
        fields.add(this.webSearchSelect);

        /* Web Search API Key */
        fields.add(label("Web Search API Key"));
        UITextbox webKey = new UITextbox((text) -> BBSSettings.aiWebSearchApiKey.set(text));
        webKey.setText(BBSSettings.aiWebSearchApiKey.get());
        webKey.h(20);
        fields.add(webKey);

        /* System Prompt */
        fields.add(label("System Prompt"));
        UITextbox sysPrompt = new UITextbox(10000, (text) -> BBSSettings.aiSystemPrompt.set(text));
        sysPrompt.setText(BBSSettings.aiSystemPrompt.get());
        sysPrompt.h(20);
        fields.add(sysPrompt);

        UIButton resetPrompt = new UIButton(IKey.raw("Reset Prompt"), (b) ->
        {
            BBSSettings.aiSystemPrompt.set(BBSSettings.DEFAULT_AI_SYSTEM_PROMPT);
            sysPrompt.setText(BBSSettings.DEFAULT_AI_SYSTEM_PROMPT);
        });
        resetPrompt.h(20);
        fields.add(resetPrompt);

        scroll.add(fields);
        this.content.add(scroll);
    }

    private void fetchModels()
    {
        String provider = BBSSettings.aiProvider.get();
        String apiKey = BBSSettings.aiApiKey.get();
        String baseUrl = BBSSettings.aiBaseUrl.get();

        this.fetchModelsButton.label = IKey.raw("...");

        AIModelFetcher.fetchModels(provider, apiKey, baseUrl).thenAccept(models ->
        {
            this.fetchModelsButton.label = IKey.raw("Fetch");
            this.fetchedModels = models;

            /* Rebuild model circulate */
            this.modelSelect.getLabels().clear();
            for (String m : models)
            {
                this.modelSelect.addLabel(IKey.raw(m));
            }

            /* Select current model if in list */
            String current = BBSSettings.aiModel.get();
            int idx = models.indexOf(current);
            this.modelSelect.setValue(Math.max(0, idx));
        });
    }

    private void refreshModelList()
    {
        /* Reset model select when provider changes */
        this.fetchedModels = null;
        this.modelSelect.getLabels().clear();
        this.modelSelect.addLabel(IKey.raw(BBSSettings.aiModel.get()));
        this.modelSelect.setValue(0);
    }

    private UILabel label(String text)
    {
        UILabel lbl = new UILabel(IKey.raw(text));
        lbl.h(14);
        lbl.color(0xFFAAAAAA);
        return lbl;
    }
}
