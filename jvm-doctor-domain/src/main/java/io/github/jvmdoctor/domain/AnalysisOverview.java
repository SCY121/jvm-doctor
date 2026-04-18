package io.github.jvmdoctor.domain;

import java.util.Map;
import java.util.Objects;

public record AnalysisOverview(
        int artifactCount,
        int threadDumpCount,
        int totalThreadCount,
        Map<String, Long> threadStateCounts,
        int actuatorMetricsFileCount,
        int availableMetricNameCount,
        int detailedMetricCount,
        int applicationLogFileCount,
        int detectedLogSignalCount
) {

    public AnalysisOverview {
        threadStateCounts = Map.copyOf(Objects.requireNonNull(threadStateCounts, "threadStateCounts must not be null"));
    }
}
