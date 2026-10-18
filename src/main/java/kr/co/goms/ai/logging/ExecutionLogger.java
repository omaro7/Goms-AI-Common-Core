package kr.co.goms.ai.logging;

public interface ExecutionLogger {

    ExecutionLogContext start(
            String runId,
            String requestId,
            String projectId,
            String toolName);

    void success(ExecutionLogContext context);

    void failure(
            ExecutionLogContext context,
            Throwable throwable);
}