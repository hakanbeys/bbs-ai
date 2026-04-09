package mchorse.bbs_mod.ui.ai;

import mchorse.bbs_mod.ai.AIOrchestrator;

/**
 * Singleton holder for the AI Orchestrator instance.
 * Ensures chat state persists across popup open/close.
 */
public class AIAnimatorManager
{
    private static final AIOrchestrator orchestrator = new AIOrchestrator();

    public static AIOrchestrator getOrchestrator()
    {
        return orchestrator;
    }
}
