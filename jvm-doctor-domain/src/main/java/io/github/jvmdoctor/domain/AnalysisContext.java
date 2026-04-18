package io.github.jvmdoctor.domain;

import java.util.List;
import java.util.Objects;

public record AnalysisContext(
        IncidentInput input,
        List<ThreadDumpSnapshot> threadDumps,
        List<ActuatorMetricsSnapshot> actuatorMetrics,
        List<ApplicationLogSnapshot> applicationLogs
) {

    public AnalysisContext {
        Objects.requireNonNull(input, "input must not be null");
        threadDumps = List.copyOf(Objects.requireNonNull(threadDumps, "threadDumps must not be null"));
        actuatorMetrics = List.copyOf(Objects.requireNonNull(actuatorMetrics, "actuatorMetrics must not be null"));
        applicationLogs = List.copyOf(Objects.requireNonNull(applicationLogs, "applicationLogs must not be null"));
    }
}
