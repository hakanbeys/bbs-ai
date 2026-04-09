package mchorse.bbs_mod.ai.session;

import mchorse.bbs_mod.ai.client.AIMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single AI chat session for a film.
 * A film can have multiple sessions (tabs).
 */
public class AISession
{
    public enum WorkflowStage
    {
        IDLE,
        DIRECTION,
        DISCOVERY,
        BLOCKING,
        CAMERA,
        PREVIEW,
        COMPLETE,
        RECOVERY
    }

    private final String id;
    private String name;
    private final List<AIMessage> messages = new ArrayList<>();
    private boolean active;
    private int revision;
    private WorkflowStage workflowStage = WorkflowStage.IDLE;
    private String workflowHint = "";
    private Integer selectedActorIndex;
    private String selectedActorName = "";
    private String selectedActorQuery = "";
    private String selectedModelKey = "";
    private String selectedPropertyPath = "";
    private String lastCameraAction = "";
    private String diagnosticSummary = "";
    private String diagnosticTracePath = "";

    public AISession(String id, String name)
    {
        this.id = id;
        this.name = name;
        this.active = false;
    }

    public String getId()
    {
        return this.id;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public List<AIMessage> getMessages()
    {
        return this.messages;
    }

    public int getRevision()
    {
        return this.revision;
    }

    public WorkflowStage getWorkflowStage()
    {
        return this.workflowStage;
    }

    public String getWorkflowHint()
    {
        return this.workflowHint;
    }

    public void setWorkflowStage(WorkflowStage workflowStage, String workflowHint)
    {
        this.workflowStage = workflowStage == null ? WorkflowStage.IDLE : workflowStage;
        this.workflowHint = workflowHint == null ? "" : workflowHint;
    }

    public Integer getSelectedActorIndex()
    {
        return this.selectedActorIndex;
    }

    public void rememberActor(Integer actorIndex, String actorName, String actorQuery)
    {
        this.selectedActorIndex = actorIndex;

        if (actorName != null && !actorName.isEmpty())
        {
            this.selectedActorName = actorName;
        }

        if (actorQuery != null && !actorQuery.isEmpty())
        {
            this.selectedActorQuery = actorQuery;
        }
    }

    public void rememberModelKey(String modelKey)
    {
        if (modelKey != null && !modelKey.isEmpty())
        {
            this.selectedModelKey = modelKey;
        }
    }

    public void rememberPropertyPath(String propertyPath)
    {
        if (propertyPath != null && !propertyPath.isEmpty())
        {
            this.selectedPropertyPath = propertyPath;
        }
    }

    public void rememberCameraAction(String cameraAction)
    {
        if (cameraAction != null && !cameraAction.isEmpty())
        {
            this.lastCameraAction = cameraAction;
        }
    }

    public String describeWorkingMemory()
    {
        StringBuilder builder = new StringBuilder();

        if (this.selectedActorIndex != null)
        {
            builder.append("- active actor: [").append(this.selectedActorIndex).append("]");

            if (!this.selectedActorName.isEmpty())
            {
                builder.append(" \"").append(this.selectedActorName).append("\"");
            }

            if (!this.selectedActorQuery.isEmpty())
            {
                builder.append(" from query \"").append(this.selectedActorQuery).append("\"");
            }

            builder.append("\n");
        }

        if (!this.selectedModelKey.isEmpty())
        {
            builder.append("- active model_key: ").append(this.selectedModelKey).append("\n");
        }

        if (!this.selectedPropertyPath.isEmpty())
        {
            builder.append("- active property_path: ").append(this.selectedPropertyPath).append("\n");
        }

        if (!this.lastCameraAction.isEmpty())
        {
            builder.append("- recent camera action: ").append(this.lastCameraAction).append("\n");
        }

        return builder.toString();
    }

    public String getDiagnosticSummary()
    {
        return this.diagnosticSummary;
    }

    public String getDiagnosticTracePath()
    {
        return this.diagnosticTracePath;
    }

    public void setDiagnostic(String summary, String tracePath)
    {
        this.diagnosticSummary = summary == null ? "" : summary;
        this.diagnosticTracePath = tracePath == null ? "" : tracePath;
        this.revision += 1;
    }

    public void addMessage(AIMessage message)
    {
        this.messages.add(message);
        this.revision += 1;
    }

    public void clearMessages()
    {
        this.messages.clear();
        this.workflowStage = WorkflowStage.IDLE;
        this.workflowHint = "";
        this.selectedActorIndex = null;
        this.selectedActorName = "";
        this.selectedActorQuery = "";
        this.selectedModelKey = "";
        this.selectedPropertyPath = "";
        this.lastCameraAction = "";
        this.diagnosticSummary = "";
        this.diagnosticTracePath = "";
        this.revision += 1;
    }

    public boolean isActive()
    {
        return this.active;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

    public boolean isEmpty()
    {
        return this.messages.isEmpty();
    }
}
