package mchorse.bbs_mod.ui.ai;

import mchorse.bbs_mod.ai.AIOrchestrator;
import mchorse.bbs_mod.ai.AIOrchestrator.OrchestratorEvent;
import mchorse.bbs_mod.ai.PromptEnhancer;
import mchorse.bbs_mod.ai.client.AIMessage;
import mchorse.bbs_mod.ai.session.AISession;
import mchorse.bbs_mod.ai.session.AISession.WorkflowStage;
import mchorse.bbs_mod.film.Film;
import net.minecraft.client.MinecraftClient;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.undo.UndoManager;

import java.util.List;

public class UIAIChatPanel extends UIOverlayPanel
{
    private final AIOrchestrator orchestrator;
    private final Film film;
    private final String filmId;
    private final UndoManager undoManager;

    private boolean alive = true;
    private boolean needsResize = false;
    private String renderedSessionId;
    private int renderedRevision = -1;

    private UIScrollView sessionSidebar;
    private UIScrollView chatScroll;
    private UIElement chatContent;
    private UIElement suggestionArea;
    private UIElement inputBar;
    private UITextbox messageInput;
    private UIButton sendButton;
    private UIButton stopButton;
    private UIButton enhanceButton;
    private UIElement approvalBar;
    private UILabel statusLabel;
    private UILabel workflowLabel;
    private UILabel memoryLabel;
    private UILabel diagnosticLabel;

    public UIAIChatPanel(AIOrchestrator orchestrator, Film film, String filmId, UndoManager undoManager)
    {
        super(IKey.raw("AI Animator"));
        this.orchestrator = orchestrator;
        this.film = film;
        this.filmId = filmId;
        this.undoManager = undoManager;

        this.orchestrator.setEventListener(this::handleEvent);
        this.buildUI();
    }

    private void buildUI()
    {
        this.sessionSidebar = new UIScrollView();
        this.sessionSidebar.relative(this.content).w(90).h(1F);
        this.sessionSidebar.column(4).vertical().stretch();

        UIButton newSession = new UIButton(IKey.raw("+ New"), (b) -> this.createNewSession());
        newSession.h(18);
        this.sessionSidebar.add(newSession);
        this.refreshSessionList();

        UIElement mainArea = new UIElement();
        mainArea.relative(this.content).x(94).w(1F, -94).h(1F);

        this.chatScroll = new UIScrollView();
        this.chatScroll.relative(mainArea).w(1F).h(1F, -94);
        this.chatScroll.column(6).vertical().stretch();

        this.chatContent = new UIElement();
        this.chatContent.column(6).vertical().stretch();
        this.chatScroll.add(this.chatContent);

        this.suggestionArea = new UIElement();
        this.suggestionArea.relative(mainArea).xy(0.5F, 0.3F).anchor(0.5F).w(1F, -20);
        this.suggestionArea.column(8).vertical().stretch();
        this.buildSuggestions();

        this.approvalBar = new UIElement();
        this.approvalBar.relative(mainArea).x(0.5F).y(1F, -68).anchor(0.5F, 0F).w(1F, -10).h(24);
        this.approvalBar.row(5);
        this.approvalBar.setVisible(false);

        this.statusLabel = new UILabel(IKey.EMPTY);
        this.statusLabel.relative(mainArea).x(0.5F).y(1F, -45).anchor(0.5F).w(1F, -10).h(14);
        this.statusLabel.labelAnchor(0.5F, 0.5F);
        this.statusLabel.color(Colors.LIGHTEST_GRAY);

        this.workflowLabel = new UILabel(IKey.EMPTY);
        this.workflowLabel.relative(mainArea).x(0.5F).y(1F, -57).anchor(0.5F).w(1F, -10).h(12);
        this.workflowLabel.labelAnchor(0.5F, 0.5F);
        this.workflowLabel.color(0xFFB8B8FF);

        this.memoryLabel = new UILabel(IKey.EMPTY);
        this.memoryLabel.relative(mainArea).x(0.5F).y(1F, -69).anchor(0.5F).w(1F, -10).h(12);
        this.memoryLabel.labelAnchor(0.5F, 0.5F);
        this.memoryLabel.color(0xFF9ED8C8);

        this.diagnosticLabel = new UILabel(IKey.EMPTY);
        this.diagnosticLabel.relative(mainArea).x(0.5F).y(1F, -81).anchor(0.5F).w(1F, -10).h(12);
        this.diagnosticLabel.labelAnchor(0.5F, 0.5F);
        this.diagnosticLabel.color(0xFFE0C58A);

        this.inputBar = new UIElement();
        this.inputBar.relative(mainArea).x(0F).y(1F, -30).w(1F).h(26);
        this.inputBar.row(4);

        this.messageInput = new UITextbox(10000, (text) -> {});
        this.messageInput.w(1F, -120).h(20);

        this.enhanceButton = new UIButton(IKey.raw("\u2728"), (b) -> this.enhancePrompt());
        this.enhanceButton.w(20).h(20);
        this.enhanceButton.tooltip(IKey.raw("Enhance prompt with scene context"), Direction.TOP);

        this.sendButton = new UIButton(IKey.raw("Send"), (b) -> this.sendMessage());
        this.sendButton.w(45).h(20);

        this.stopButton = new UIButton(IKey.raw("Stop"), (b) -> this.stopGeneration());
        this.stopButton.w(45).h(20);
        this.stopButton.setVisible(false);

        this.inputBar.add(this.messageInput, this.enhanceButton, this.sendButton);
        mainArea.add(this.chatScroll, this.suggestionArea, this.approvalBar, this.statusLabel, this.workflowLabel, this.memoryLabel, this.diagnosticLabel, this.inputBar);
        this.content.add(this.sessionSidebar, mainArea);

        AISession active = this.orchestrator.getSessionManager().getActiveSession(this.filmId);

        if (active != null && !active.isEmpty())
        {
            this.loadSessionMessages(active);
            this.suggestionArea.setVisible(false);
        }
        else
        {
            this.refreshWorkflowLabel(active);
        }

        if (this.orchestrator.isRunning())
        {
            this.showStopButton();
            this.statusLabel.label = IKey.raw("Thinking...");
        }
    }

    private void buildSuggestions()
    {
        this.suggestionArea.removeAll();

        UILabel title = new UILabel(IKey.raw("AI Animator"));
        title.h(18);
        title.labelAnchor(0.5F, 0.5F);
        title.color(Colors.WHITE, true);

        UILabel subtitle = new UILabel(IKey.raw("What would you like to animate?"));
        subtitle.h(14);
        subtitle.labelAnchor(0.5F, 0.5F);
        subtitle.color(Colors.LIGHTEST_GRAY);

        this.suggestionArea.add(title, subtitle);

        String[][] suggestions = {
            {"Walk Cycle", "Create a smooth looping walk cycle animation with natural body sway and head bob"},
            {"Idle Breathing", "Create a subtle idle breathing animation with gentle head movement"},
            {"Fight Scene", "Create a dramatic fight scene between 2 characters with attack combos, dodges, and cinematic camera angles"},
            {"Cinematic Entrance", "Actor walks dramatically toward camera from a distance with establishing wide shot"},
            {"Run & Jump", "Create a running and jumping sequence with smooth takeoff and landing"},
            {"Full Scene", "Generate a complete cinematic scene: 2 actors interacting with multiple camera angles"}
        };

        UIElement row = null;

        for (int i = 0; i < suggestions.length; i++)
        {
            if (i % 3 == 0)
            {
                row = new UIElement();
                row.row(6);
                this.suggestionArea.add(row);
            }

            String description = suggestions[i][1];
            UIButton card = new UIButton(IKey.raw(suggestions[i][0]), (b) ->
            {
                this.messageInput.setText(description);
                this.sendMessage();
            });
            card.h(24);
            card.tooltip(IKey.raw(description), Direction.BOTTOM);
            row.add(card);
        }
    }

    private void sendMessage()
    {
        String text = this.messageInput.textbox.getText().trim();
        this.sendMessage(text, true);
    }

    private void sendMessage(String text, boolean clearInput)
    {
        if (text.isEmpty() || this.orchestrator.isRunning())
        {
            return;
        }

        if (clearInput)
        {
            this.messageInput.setText("");
        }

        this.suggestionArea.setVisible(false);
        this.showStopButton();
        this.statusLabel.label = IKey.raw("Thinking...");
        this.orchestrator.sendMessage(text, this.film, this.filmId, this.undoManager);
    }

    private void enhancePrompt()
    {
        String text = this.messageInput.textbox.getText().trim();

        if (!text.isEmpty())
        {
            this.messageInput.setText(PromptEnhancer.enhance(text, this.film));
        }
    }

    private void stopGeneration()
    {
        this.orchestrator.cancel();
        this.statusLabel.label = IKey.raw("Cancelled.");
        this.showSendButton();
    }

    private void createNewSession()
    {
        List<AISession> sessions = this.orchestrator.getSessionManager().getSessions(this.filmId);
        this.orchestrator.getSessionManager().createSession(this.filmId, "Chat " + (sessions.size() + 1));
        this.renderedSessionId = null;
        this.renderedRevision = -1;
        this.chatContent.removeAll();
        this.suggestionArea.setVisible(true);
        this.refreshSessionList();
        this.needsResize = true;
    }

    private void refreshSessionList()
    {
        while (this.sessionSidebar.getChildren().size() > 1)
        {
            this.sessionSidebar.getChildren().remove(1);
        }

        for (AISession session : this.orchestrator.getSessionManager().getSessions(this.filmId))
        {
            UIButton tab = new UIButton(IKey.raw(session.getName()), (b) ->
            {
                this.orchestrator.getSessionManager().setActiveSession(this.filmId, session);
                this.loadSessionMessages(session);
                this.refreshSessionList();
                this.needsResize = true;
            });
            tab.h(18);

            if (session.isActive())
            {
                tab.color(Colors.LIGHTEST_GRAY & Colors.RGB);
            }

            this.sessionSidebar.add(tab);
        }
    }

    private void loadSessionMessages(AISession session)
    {
        this.chatContent.removeAll();
        this.renderedSessionId = session.getId();
        this.renderedRevision = session.getRevision();

        for (AIMessage message : session.getMessages())
        {
            this.renderSessionMessage(message);
        }

        this.suggestionArea.setVisible(session.isEmpty());
        this.refreshWorkflowLabel(session);
        this.needsResize = true;
    }

    private boolean renderSessionMessage(AIMessage message)
    {
        String role = message.getRole().name().toLowerCase();
        String content = message.getContent();

        if (role.equals("system") || content == null || content.isEmpty())
        {
            return false;
        }

        if (role.equals("tool"))
        {
            this.addToolResultElement(message.getToolName(), content, !content.startsWith("ERROR"));
        }
        else
        {
            this.addChatElement(role, content, true);
        }

        return true;
    }

    private void addChatElement(String role, String text, boolean success)
    {
        if (role.equals("assistant"))
        {
            List<UIAIMessageParser.Segment> segments = UIAIMessageParser.parse(text);

            if (!segments.isEmpty())
            {
                for (UIAIMessageParser.Segment segment : segments)
                {
                    this.addAssistantSegment(segment, success);
                }

                this.needsResize = true;

                return;
            }
        }

        UIAIChatBubble bubble = new UIAIChatBubble(role, text, success);
        bubble.w(1F);
        this.chatContent.add(bubble);
        this.needsResize = true;
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

    private void addToolResultElement(String toolName, String content, boolean success)
    {
        String prefix = success ? "[OK] " : "[ERR] ";
        UIAIChatBubble bubble = new UIAIChatBubble("tool", prefix + (toolName == null ? "tool" : toolName) + ": " + content, success);
        bubble.w(1F);
        this.chatContent.add(bubble);
        this.needsResize = true;
    }

    private void showStopButton()
    {
        this.sendButton.setVisible(false);
        this.stopButton.setVisible(true);
        this.inputBar.removeAll();
        this.inputBar.add(this.messageInput, this.enhanceButton, this.stopButton);
    }

    private void showSendButton()
    {
        this.sendButton.setVisible(true);
        this.stopButton.setVisible(false);
        this.inputBar.removeAll();
        this.inputBar.add(this.messageInput, this.enhanceButton, this.sendButton);
    }

    private void showApproval(int count)
    {
        this.approvalBar.removeAll();

        UILabel label = new UILabel(IKey.raw(count + " changes ready"));
        label.w(1F, -130).h(20);
        label.labelAnchor(0.5F, 0.5F);

        UIButton approve = new UIButton(IKey.raw("Approve"), (b) ->
        {
            this.orchestrator.approveChanges();
            this.approvalBar.setVisible(false);
            this.addChatElement("system", "Changes approved.", true);
        });
        approve.w(60).h(20);

        UIButton reject = new UIButton(IKey.raw("Reject"), (b) ->
        {
            this.orchestrator.rejectChanges(this.film, this.undoManager);
            this.approvalBar.setVisible(false);
            this.addChatElement("system", "Changes rejected.", false);
        });
        reject.w(60).h(20);

        this.approvalBar.add(label, approve, reject);
        this.approvalBar.setVisible(true);
        this.needsResize = true;
    }

    private void handleEvent(OrchestratorEvent event)
    {
        if (!this.alive)
        {
            return;
        }

        MinecraftClient.getInstance().execute(() ->
        {
            if (this.alive)
            {
                this.handleEventOnMainThread(event);
            }
        });
    }

    private void handleEventOnMainThread(OrchestratorEvent event)
    {
        switch (event.type)
        {
            case USER_MESSAGE:
                this.syncChatFromActiveSession();
                break;
            case ASSISTANT_MESSAGE:
                this.syncChatFromActiveSession();
                break;
            case TOOL_CALL:
                this.statusLabel.label = IKey.raw("Running " + event.toolName + "...");
                break;
            case TOOL_RESULT:
                this.syncChatFromActiveSession();
                break;
            case APPROVAL_REQUIRED:
                this.showApproval(event.changeCount);
                break;
            case ERROR:
                this.addChatElement("system", "Error: " + event.content, false);
                this.statusLabel.label = IKey.raw("Error occurred.");
                this.showSendButton();
                break;
            case THINKING:
                this.statusLabel.label = IKey.raw("Thinking...");
                break;
            case FINISHED:
                this.statusLabel.label = IKey.EMPTY;
                this.showSendButton();
                this.refreshSessionList();
                this.syncChatFromActiveSession();
                break;
            case PREVIEW_REQUESTED:
                this.statusLabel.label = IKey.raw("Preview from tick " + event.fromTick);
                break;
            default:
                break;
        }
    }

    private void syncChatFromActiveSession()
    {
        AISession active = this.orchestrator.getSessionManager().getActiveSession(this.filmId);

        if (active == null)
        {
            this.refreshWorkflowLabel(null);
            return;
        }

        this.refreshWorkflowLabel(active);

        if (!active.getId().equals(this.renderedSessionId) || active.getRevision() != this.renderedRevision || this.chatContent.getChildren().isEmpty() && !active.isEmpty())
        {
            this.loadSessionMessages(active);
        }
    }

    private void refreshWorkflowLabel(AISession session)
    {
        if (session == null)
        {
            this.workflowLabel.label = IKey.EMPTY;
            this.memoryLabel.label = IKey.EMPTY;
            this.diagnosticLabel.label = IKey.EMPTY;
            return;
        }

        WorkflowStage stage = session.getWorkflowStage();
        String hint = session.getWorkflowHint();

        if (stage == null || stage == WorkflowStage.IDLE)
        {
            this.workflowLabel.label = IKey.EMPTY;
        }
        else
        {
            String label = "Stage: " + this.formatStage(stage);

            if (hint != null && !hint.isEmpty())
            {
                label += " | " + hint;
            }

            this.workflowLabel.label = IKey.raw(label);
        }

        String memory = session.describeWorkingMemory().trim();

        if (memory.isEmpty())
        {
            this.memoryLabel.label = IKey.EMPTY;
        }
        else
        {
            this.memoryLabel.label = IKey.raw("Memory: " + memory.replace("\n", " | ").trim());
        }

        String diagnostic = session.getDiagnosticSummary();

        if (diagnostic == null || diagnostic.isEmpty())
        {
            this.diagnosticLabel.label = IKey.EMPTY;
        }
        else
        {
            this.diagnosticLabel.label = IKey.raw(diagnostic);
        }
    }

    private String formatStage(WorkflowStage stage)
    {
        switch (stage)
        {
            case DIRECTION: return "Direction";
            case DISCOVERY: return "Discovery";
            case BLOCKING: return "Blocking";
            case CAMERA: return "Camera";
            case PREVIEW: return "Preview";
            case COMPLETE: return "Complete";
            case RECOVERY: return "Recovery";
            default: return "Idle";
        }
    }

    @Override
    public void onClose()
    {
        this.alive = false;
        this.orchestrator.setEventListener(null);
        super.onClose();
    }

    @Override
    public void render(UIContext context)
    {
        this.syncChatFromActiveSession();

        if (this.needsResize)
        {
            this.needsResize = false;
            this.resize();
            this.chatScroll.scroll.scrollTo(Integer.MAX_VALUE);
        }

        this.sessionSidebar.area.render(context.batcher, Colors.A25);
        super.render(context);
    }
}
