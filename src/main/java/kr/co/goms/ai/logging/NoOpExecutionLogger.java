package kr.co.goms.ai.logging;

import java.time.Instant;

public class NoOpExecutionLogger implements ExecutionLogger {

    @Override
    public ExecutionLogContext start(
            String runId,
            String requestId,
            String projectId,
            String toolName) {

        return new ExecutionLogContext(
                -1L,
                Instant.now());
    }

    @Override
    public void success(ExecutionLogContext context) {
    }

    @Override
    public void failure(
            ExecutionLogContext context,
            Throwable throwable) {
    }
}