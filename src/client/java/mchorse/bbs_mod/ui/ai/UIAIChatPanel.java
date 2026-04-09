package mchorse.bbs_mod.ui.ai;

import mchorse.bbs_mod.ai.AIOrchestrator;
import mchorse.bbs_mod.ai.AIOrchestrator.OrchestratorEvent;
import mchorse.bbs_mod.ai.session.AISession;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.List;

/**
 * AI Animator dashboard panel.
 * Contains chat interface, session tabs, suggestion cards, and settings access.
 */
public class UIAIChatPanel extends UIDashboardPanel
{
    /* Core AI engine */
    private final AIOrchestrator orchestrator = new AIOrchestrator();

    /* UI elements */
    private UIElement sessionTabs;
    private UIScrollView chatScroll;
    private UIElement chatContent;
    private UIElement suggestionArea;
    private UIElement inputBar;
    private UITextbox messageInput;
    private UIButton sendButton;
    private UIButton stopButton;
    private UIElement approvalBar;
    private UILabel statusLabel;

    /* Top bar icons */
    private UIIcon settingsIcon;
    private UIIcon clearIcon;
    private UIIcon newSessionIcon;

    /* Current film reference */
    private Film currentFilm;
    private String currentFilmId;

    public UIAIChatPanel(UIDashboard dashboard)
    {
        super(dashboard);
        this.markContainer();

        /* Setup orchestrator event listener */
        this.orchestrator.setEventListener(this::handleOrchestratorEvent);

        this.buildUI();
    }

    private void buildUI()
    {
        /* === Top bar: session tabs + icons === */
        UIElement topBar = new UIElement();
        topBar.relative(this).w(1F).h(24);
        topBar.row(2);

        /* Session tabs container */
        this.sessionTabs = new UIElement();
        this.sessionTabs.row(2);
        this.sessionTabs.w(1F, -80);
        topBar.add(this.sessionTabs);

        /* Top bar icons */
        this.newSessionIcon = new UIIcon(Icons.ADD, (b) -> this.createNewSession());
        this.newSessionIcon.tooltip(IKey.raw("New Session"), Direction.BOTTOM);
        this.clearIcon = new UIIcon(Icons.CLOSE, (b) -> this.clearChat());
        this.clearIcon.tooltip(IKey.raw("Clear Chat"), Direction.BOTTOM);
        this.settingsIcon = new UIIcon(Icons.SETTINGS, (b) -> this.openSettings());
        this.settingsIcon.tooltip(IKey.raw("AI Settings"), Direction.BOTTOM);

        UIElement iconsRow = new UIElement();
        iconsRow.row(2);
        iconsRow.w(75).h(20);
        iconsRow.add(this.newSessionIcon, this.clearIcon, this.settingsIcon);
        topBar.add(iconsRow);

        /* === Chat scroll area === */
        this.chatScroll = new UIScrollView();
        this.chatScroll.relative(this).y(28).w(1F).h(1F, -80);
        this.chatScroll.column(8).vertical().stretch();

        /* Chat content */
        this.chatContent = new UIElement();
        this.chatContent.column(8).vertical().stretch();
        this.chatScroll.add(this.chatContent);

        /* === Suggestion cards area (shown when chat is empty) === */
        this.suggestionArea = new UIElement();
        this.suggestionArea.relative(this).xy(0.5F, 0.4F).anchor(0.5F).w(1F, -40);
        this.suggestionArea.column(8).vertical().stretch();
        this.buildSuggestionCards();

        /* === Approval bar (hidden by default) === */
        this.approvalBar = new UIElement();
        this.approvalBar.relative(this).x(0.5F).y(1F, -80).anchor(0.5F, 0F).w(1F, -20).h(30);
        this.approvalBar.row(5);
        this.approvalBar.setVisible(false);

        /* === Status label === */
        this.statusLabel = new UILabel(IKey.EMPTY);
        this.statusLabel.relative(this).x(0.5F).y(1F, -55).anchor(0.5F).w(1F, -20).h(15);
        this.statusLabel.labelAnchor(0.5F, 0.5F);

        /* === Input bar: text input + send/stop === */
        this.inputBar = new UIElement();
        this.inputBar.relative(this).x(0F).y(1F, -40).w(1F).h(36);
        this.inputBar.row(5);

        this.messageInput = new UITextbox(10000, (text) -> {});
        this.messageInput.w(1F, -75).h(24);

        this.sendButton = new UIButton(IKey.raw("Send"), (b) -> this.sendMessage());
        this.sendButton.w(60).h(24);

        this.stopButton = new UIButton(IKey.raw("Stop"), (b) -> this.stopGeneration());
        this.stopButton.w(60).h(24);
        this.stopButton.setVisible(false);

        this.inputBar.add(this.messageInput, this.sendButton);

        /* Add all to panel */
        this.add(topBar);
        this.add(this.chatScroll);
        this.add(this.suggestionArea);
        this.add(this.approvalBar);
        this.add(this.statusLabel);
        this.add(this.inputBar);
    }

    private void buildSuggestionCards()
    {
        this.suggestionArea.removeAll();

        UILabel title = new UILabel(IKey.raw("AI Animator"));
        title.h(20);
        title.labelAnchor(0.5F, 0.5F);
        title.color(Colors.WHITE, true);

        UILabel subtitle = new UILabel(IKey.raw("What would you like to animate?"));
        subtitle.h(16);
        subtitle.labelAnchor(0.5F, 0.5F);
        subtitle.color(Colors.LIGHTEST_GRAY);

        this.suggestionArea.add(title, subtitle);

        /* Suggestion cards row */
        UIElement cardsRow1 = new UIElement();
        cardsRow1.row(8);

        cardsRow1.add(createSuggestionCard("Walk Cycle", "Create a looping walk animation"));
        cardsRow1.add(createSuggestionCard("Idle Animation", "Create a subtle idle breathing animation"));
        cardsRow1.add(createSuggestionCard("Fight Scene", "Create a combat animation sequence"));

        UIElement cardsRow2 = new UIElement();
        cardsRow2.row(8);

        cardsRow2.add(createSuggestionCard("Cinematic Walk", "Actor walks toward camera dramatically"));
        cardsRow2.add(createSuggestionCard("Jump", "Create a jump animation with landing"));
        cardsRow2.add(createSuggestionCard("Run Cycle", "Create a looping run animation"));

        this.suggestionArea.add(cardsRow1, cardsRow2);
    }

    private UIButton createSuggestionCard(String title, String description)
    {
        UIButton card = new UIButton(IKey.raw(title), (b) ->
        {
            this.messageInput.setText(description);
            this.sendMessage();
        });
        card.h(28);
        card.tooltip(IKey.raw(description), Direction.BOTTOM);
        return card;
    }

    /* === Actions === */

    private void sendMessage()
    {
        String text = this.messageInput.textbox.getText().trim();
        this.sendMessage(text, true);
    }

    private void sendMessage(String text, boolean clearInput)
    {
        if (text.isEmpty()) return;
        if (this.orchestrator.isRunning()) return;

        if (clearInput)
        {
            this.messageInput.setText("");
        }

        this.suggestionArea.setVisible(false);

        /* Get current film from film panel */
        this.resolveFilm();

        if (this.currentFilm == null)
        {
            this.addChatMessage("system", "No film is open. Open a film first.", false);
            return;
        }

        /* Add user message to chat visually */
        this.addChatMessage("user", text, true);

        /* Switch buttons */
        this.sendButton.setVisible(false);
        this.stopButton.setVisible(true);
        this.inputBar.add(this.stopButton);
        this.statusLabel.label = IKey.raw("Thinking...");

        /* Send to orchestrator */
        UIFilmPanel filmPanel = this.dashboard.getPanel(UIFilmPanel.class);
        this.orchestrator.sendMessage(
            text,
            this.currentFilm,
            this.currentFilmId,
            filmPanel != null ? filmPanel.getUndoHandler().getUndoManager() : null
        );
    }

    private void stopGeneration()
    {
        this.orchestrator.cancel();
        this.statusLabel.label = IKey.raw("Cancelled.");
        this.resetInputState();
    }

    private void clearChat()
    {
        AISession session = this.orchestrator.getSessionManager().getActiveSession(this.currentFilmId);
        if (session != null)
        {
            session.clearMessages();
        }
        this.chatContent.removeAll();
        this.suggestionArea.setVisible(true);
        this.statusLabel.label = IKey.EMPTY;
    }

    private void createNewSession()
    {
        if (this.currentFilmId == null) return;

        List<AISession> sessions = this.orchestrator.getSessionManager().getSessions(this.currentFilmId);
        String name = "Chat " + (sessions.size() + 1);
        this.orchestrator.getSessionManager().createSession(this.currentFilmId, name);
        this.chatContent.removeAll();
        this.suggestionArea.setVisible(true);
        this.updateSessionTabs();
    }

    private void openSettings()
    {
        UIAISettingsOverlay overlay = new UIAISettingsOverlay();
        UIOverlay.addOverlayRight(this.getContext(), overlay, 280);
    }

    private void approveChanges()
    {
        this.orchestrator.approveChanges();
        this.approvalBar.setVisible(false);
        this.addChatMessage("system", "Changes approved.", true);
    }

    private void rejectChanges()
    {
        UIFilmPanel filmPanel = this.dashboard.getPanel(UIFilmPanel.class);
        this.orchestrator.rejectChanges(this.currentFilm,
            filmPanel != null ? filmPanel.getUndoHandler().getUndoManager() : null);
        this.approvalBar.setVisible(false);
        this.addChatMessage("system", "Changes rejected and undone.", false);
    }

    /* === Chat rendering === */

    private void addChatMessage(String role, String text, boolean success)
    {
        if ("assistant".equals(role))
        {
            List<UIAIMessageParser.Segment> segments = UIAIMessageParser.parse(text);

            if (!segments.isEmpty())
            {
                for (UIAIMessageParser.Segment segment : segments)
                {
                    this.addAssistantSegment(segment, success);
                }

                this.finishChatAppend();

                return;
            }
        }

        UIAIChatBubble bubble = new UIAIChatBubble(role, text, success);
        bubble.w(1F);
        this.chatContent.add(bubble);
        this.finishChatAppend();
    }

    private void addToolCallMessage(String toolName, String content, boolean success)
    {
        String prefix = success ? "[OK] " : "[ERR] ";
        UIAIChatBubble bubble = new UIAIChatBubble("tool", prefix + toolName + ": " + content, success);
        bubble.w(1F);
        this.chatContent.add(bubble);
        this.finishChatAppend();
    }

    private void addAssistantSegment(UIAIMessageParser.Segment segment, boolean success)
    {
        UIElement element = null;

        if (segment.type == UIAIMessageParser.SegmentType.TEXT)
        {
            element = new UIAIChatBubble("assistant", segment.text, success);
        }
        else if (segment.type == UIAIMessageParser.SegmentType.QUESTION)
        {
            element = new UIAIQuestionCard(segment.question, (answer) -> this.sendMessage(answer, false));
        }
        else if (segment.type == UIAIMessageParser.SegmentType.TODO)
        {
            element = new UIAITodoCard(segment.todo);
        }

        if (element != null)
        {
            element.w(1F);
            this.chatContent.add(element);
        }
    }

    private void finishChatAppend()
    {
        this.chatScroll.scroll.scrollTo(Integer.MAX_VALUE);
        this.resize();
    }

    /* === Orchestrator events === */

    private void handleOrchestratorEvent(OrchestratorEvent event)
    {
        /* Events come from a background thread — schedule UI update */
        switch (event.type)
        {
            case ASSISTANT_MESSAGE:
                this.addChatMessage("assistant", event.content, true);
                break;

            case TOOL_CALL:
                this.statusLabel.label = IKey.raw("Running " + event.toolName + "...");
                break;

            case TOOL_RESULT:
                this.addToolCallMessage(event.toolName, event.content, event.success);
                break;

            case APPROVAL_REQUIRED:
                this.showApprovalBar(event.changeCount);
                break;

            case APPROVED:
                this.addChatMessage("system", event.changeCount + " changes approved.", true);
                break;

            case REJECTED:
                this.addChatMessage("system", event.changeCount + " changes rejected.", false);
                break;

            case ERROR:
                this.addChatMessage("system", "Error: " + event.content, false);
                this.statusLabel.label = IKey.raw("Error occurred.");
                this.resetInputState();
                break;

            case THINKING:
                this.statusLabel.label = IKey.raw("Thinking...");
                break;

            case FINISHED:
                this.statusLabel.label = IKey.EMPTY;
                this.resetInputState();
                break;

            case PREVIEW_REQUESTED:
                this.statusLabel.label = IKey.raw("Preview from tick " + event.fromTick);
                /* TODO: trigger playback in film panel */
                break;
        }
    }

    private void showApprovalBar(int changeCount)
    {
        this.approvalBar.removeAll();

        UILabel label = new UILabel(IKey.raw(changeCount + " changes ready"));
        label.w(1F, -140).h(24);
        label.labelAnchor(0.5F, 0.5F);

        UIButton approveBtn = new UIButton(IKey.raw("Approve"), (b) -> this.approveChanges());
        approveBtn.w(65).h(24);

        UIButton rejectBtn = new UIButton(IKey.raw("Reject"), (b) -> this.rejectChanges());
        rejectBtn.w(65).h(24);

        this.approvalBar.add(label, approveBtn, rejectBtn);
        this.approvalBar.setVisible(true);
        this.resize();
    }

    private void resetInputState()
    {
        this.sendButton.setVisible(true);
        this.stopButton.setVisible(false);

        this.inputBar.removeAll();
        this.inputBar.add(this.messageInput, this.sendButton);
    }

    /* === Session tabs === */

    private void updateSessionTabs()
    {
        this.sessionTabs.removeAll();

        if (this.currentFilmId == null) return;

        List<AISession> sessions = this.orchestrator.getSessionManager().getSessions(this.currentFilmId);
        for (AISession session : sessions)
        {
            UIButton tab = new UIButton(IKey.raw(session.getName()), (b) ->
            {
                this.orchestrator.getSessionManager().setActiveSession(this.currentFilmId, session);
                this.loadSessionMessages(session);
                this.updateSessionTabs();
            });
            tab.w(70).h(20);
            if (session.isActive())
            {
                tab.color(Colors.LIGHTEST_GRAY & Colors.RGB);
            }
            this.sessionTabs.add(tab);
        }
    }

    private void loadSessionMessages(AISession session)
    {
        this.chatContent.removeAll();

        for (var msg : session.getMessages())
        {
            String role = msg.getRole().name().toLowerCase();

            if ("tool".equals(role))
            {
                String toolName = msg.getToolName() == null ? "tool" : msg.getToolName();
                this.addToolCallMessage(toolName, msg.getContent(), true);
            }
            else
            {
                this.addChatMessage(role, msg.getContent(), true);
            }
        }

        this.suggestionArea.setVisible(session.isEmpty());
    }

    /* === Film resolution === */

    private void resolveFilm()
    {
        UIFilmPanel filmPanel = this.dashboard.getPanel(UIFilmPanel.class);
        if (filmPanel != null)
        {
            this.currentFilm = filmPanel.getData();
            this.currentFilmId = this.currentFilm != null ? "film_" + this.currentFilm.hashCode() : null;
        }
    }

    /* === Lifecycle === */

    @Override
    public void appear()
    {
        super.appear();
        this.resolveFilm();
        this.updateSessionTabs();
    }

    @Override
    public void renderPanelBackground(UIContext context)
    {
        super.renderPanelBackground(context);

        /* Dark background for chat area */
        this.chatScroll.area.render(context.batcher, Colors.A50);
    }

    public AIOrchestrator getOrchestrator()
    {
        return this.orchestrator;
    }
}
