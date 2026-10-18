package kr.co.goms.ai.agent.event;

public interface AgentEventPublisher {

    void publish(
            AgentEvent event
    );
}