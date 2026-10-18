package kr.co.goms.ai.agent.event;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DefaultAgentEventPublisher
        implements AgentEventPublisher {

    private final List<AgentEventListener> listeners =
            new CopyOnWriteArrayList<>();

    public void addListener(
            AgentEventListener listener) {

        listeners.add(
                Objects.requireNonNull(
                        listener,
                        "listener must not be null"
                )
        );
    }

    public void removeListener(
            AgentEventListener listener) {

        listeners.remove(
                listener
        );
    }

    @Override
    public void publish(
            AgentEvent event) {

        Objects.requireNonNull(
                event,
                "event must not be null"
        );

        for (AgentEventListener listener
                : listeners) {

            try {

                listener.onEvent(
                        event
                );

            } catch (RuntimeException exception) {

                System.err.println(
                        "[GomsBook AI] Agent Event Listener Failed"
                                + " | type="
                                + event.getType()
                                + " | message="
                                + exception.getMessage()
                );
            }
        }
    }
}