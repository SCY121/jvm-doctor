package io.github.jvmdoctor.engine;

import io.github.jvmdoctor.domain.Artifact;
import io.github.jvmdoctor.domain.ArtifactType;
import io.github.jvmdoctor.domain.IncidentInput;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentAnalysisEngineTest {

    @Test
    void shouldBuildMinimalAnalysisOverviewFromArtifacts() {
        IncidentInput input = new IncidentInput(
                List.of(
                        new Artifact(
                                ArtifactType.THREAD_DUMP,
                                "thread-dump",
                                Path.of("../jvm-doctor-parser/src/test/resources/thread-dump-sample.txt")
                        ),
                        new Artifact(
                                ArtifactType.ACTUATOR_METRICS,
                                "actuator-metrics",
                                Path.of("../jvm-doctor-parser/src/test/resources/actuator-metrics-sample.json")
                        )
                ),
                Map.of("source", "engine-test")
        );

        var result = IncidentAnalysisEngine.bootstrap().analyze(input);

        assertThat(result.overview().threadDumpCount()).isEqualTo(1);
        assertThat(result.overview().totalThreadCount()).isEqualTo(3);
        assertThat(result.overview().availableMetricNameCount()).isEqualTo(3);
        assertThat(result.report().executiveSummary()).contains("Parsed 1 thread dump file(s)");
    }
}
