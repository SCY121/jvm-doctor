package io.github.jvmdoctor.bench;

import io.github.jvmdoctor.engine.IncidentAnalysisEngine;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CorpusBenchmarkRunnerTest {

    @Test
    void shouldValidateSampleCorpusAgainstExpectations() {
        Path corpusRoot = Path.of("..", "samples", "incidents").toAbsolutePath().normalize();

        CorpusBenchmarkReport report = new CorpusBenchmarkRunner(IncidentAnalysisEngine.bootstrap()).run(corpusRoot);

        assertThat(report.totalCases()).isEqualTo(4);
        assertThat(report.passedCases()).hasSize(4);
        assertThat(report.failedCases()).isEmpty();
    }
}
