package kr.co.goms.ai.agent.event;

public interface AgentEventListener {

    void onEvent(
            AgentEvent event
    );
}