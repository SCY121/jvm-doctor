package io.github.jvmdoctor.domain;

import java.util.List;
import java.util.Objects;

public record ActuatorMetric(String name, List<ActuatorMetricMeasurement> measurements) {

    public ActuatorMetric {
        Objects.requireNonNull(name, "name 不能为空");
        measurements = List.copyOf(Objects.requireNonNull(measurements, "measurements 不能为空"));
    }
}

