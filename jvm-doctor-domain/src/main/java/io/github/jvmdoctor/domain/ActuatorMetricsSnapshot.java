package io.github.jvmdoctor.domain;

import java.util.List;
import java.util.Objects;

public record ActuatorMetricsSnapshot(
        List<String> metricNames,
        List<ActuatorMetric> detailedMetrics
) {

    public ActuatorMetricsSnapshot {
        metricNames = List.copyOf(Objects.requireNonNull(metricNames, "metricNames 不能为空"));
        detailedMetrics = List.copyOf(Objects.requireNonNull(detailedMetrics, "detailedMetrics 不能为空"));
    }

    public int metricNameCount() {
        return metricNames.size();
    }

    public int detailedMetricCount() {
        return detailedMetrics.size();
    }
}

