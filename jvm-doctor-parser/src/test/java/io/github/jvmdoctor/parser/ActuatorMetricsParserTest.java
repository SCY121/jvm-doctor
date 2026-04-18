package io.github.jvmdoctor.parser;

import io.github.jvmdoctor.domain.Artifact;
import io.github.jvmdoctor.domain.ArtifactType;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorMetricsParserTest {

    private final ActuatorMetricsParser parser = new ActuatorMetricsParser();

    @Test
    void shouldParseMetricNamesAndDetailedMeasurements() {
        Artifact artifact = new Artifact(
                ArtifactType.ACTUATOR_METRICS,
                "sample-actuator-metrics",
                Path.of("src/test/resources/actuator-metrics-sample.json")
        );

        var snapshot = parser.parse(artifact);

        assertThat(snapshot.metricNameCount()).isEqualTo(3);
        assertThat(snapshot.detailedMetricCount()).isEqualTo(2);
        assertThat(snapshot.metricNames()).contains("jvm.threads.live", "hikaricp.connections.active", "jvm.gc.pause");
        assertThat(snapshot.detailedMetrics().getFirst().measurements().getFirst().value()).isEqualTo(42.0);
    }
}

