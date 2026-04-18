package io.github.jvmdoctor.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvmdoctor.domain.ActuatorMetric;
import io.github.jvmdoctor.domain.ActuatorMetricMeasurement;
import io.github.jvmdoctor.domain.ActuatorMetricsSnapshot;
import io.github.jvmdoctor.domain.Artifact;
import io.github.jvmdoctor.domain.ArtifactType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public final class ActuatorMetricsParser implements ArtifactParser<ActuatorMetricsSnapshot> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(ArtifactType artifactType) {
        return artifactType == ArtifactType.ACTUATOR_METRICS;
    }

    @Override
    public ActuatorMetricsSnapshot parse(Artifact artifact) {
        try {
            JsonNode root = objectMapper.readTree(artifact.path().toFile());
            return toSnapshot(root);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read actuator metrics: " + artifact.path(), exception);
        }
    }

    private ActuatorMetricsSnapshot toSnapshot(JsonNode root) {
        List<String> metricNames = new ArrayList<>();
        List<ActuatorMetric> detailedMetrics = new ArrayList<>();

        if (root.isArray()) {
            root.forEach(node -> detailedMetrics.add(readMetric(node)));
        } else {
            if (root.has("names")) {
                root.get("names").forEach(node -> metricNames.add(node.asText()));
            }
            if (root.has("metrics")) {
                root.get("metrics").forEach(node -> detailedMetrics.add(readMetric(node)));
            }
            if (root.has("name")) {
                detailedMetrics.add(readMetric(root));
                String metricName = root.path("name").asText();
                if (!metricName.isBlank() && !metricNames.contains(metricName)) {
                    metricNames.add(metricName);
                }
            }
        }

        for (ActuatorMetric metric : detailedMetrics) {
            if (!metricNames.contains(metric.name())) {
                metricNames.add(metric.name());
            }
        }

        return new ActuatorMetricsSnapshot(metricNames, detailedMetrics);
    }

    private ActuatorMetric readMetric(JsonNode node) {
        String name = node.path("name").asText("unknown.metric");
        List<ActuatorMetricMeasurement> measurements = new ArrayList<>();
        JsonNode measurementNodes = node.path("measurements");
        if (measurementNodes.isArray()) {
            measurementNodes.forEach(measurement -> measurements.add(new ActuatorMetricMeasurement(
                    measurement.path("statistic").asText("VALUE"),
                    measurement.path("value").asDouble(0.0)
            )));
        }
        return new ActuatorMetric(name, measurements);
    }
}
