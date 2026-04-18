package io.github.jvmdoctor.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ThreadDumpSnapshot(
        int threadCount,
        boolean deadlockDetected,
        Map<String, Long> stateCounts,
        List<ThreadDumpThread> threads
) {

    public ThreadDumpSnapshot {
        stateCounts = Map.copyOf(Objects.requireNonNull(stateCounts, "stateCounts must not be null"));
        threads = List.copyOf(Objects.requireNonNull(threads, "threads must not be null"));
    }
}
