package kr.co.goms.ai.logging;

import java.time.Instant;

public record ExecutionLogContext(
        long logId,
        Instant startedAt) {
}