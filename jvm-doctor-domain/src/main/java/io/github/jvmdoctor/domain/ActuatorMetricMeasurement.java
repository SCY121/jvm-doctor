package io.github.jvmdoctor.domain;

import java.util.Objects;

public record ActuatorMetricMeasurement(String statistic, double value) {

    public ActuatorMetricMeasurement {
        Objects.requireNonNull(statistic, "statistic 不能为空");
    }
}

