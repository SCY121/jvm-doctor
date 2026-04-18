package io.github.jvmdoctor.parser;

import io.github.jvmdoctor.domain.ApplicationLogSignalType;
import io.github.jvmdoctor.domain.Artifact;
import io.github.jvmdoctor.domain.ArtifactType;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationLogParserTest {

    private final ApplicationLogParser parser = new ApplicationLogParser();

    @Test
    void shouldDetectKnownSignals() {
        Artifact artifact = new Artifact(
                ArtifactType.APPLICATION_LOG,
                "sample-log",
                Path.of("src/test/resources/application-log-sample.log")
        );

        var snapshot = parser.parse(artifact);

        assertThat(snapshot.lineCount()).isEqualTo(4);
        assertThat(snapshot.signals())
                .extracting(signal -> signal.type())
                .contains(
                        ApplicationLogSignalType.DB_CONNECTION_TIMEOUT,
                        ApplicationLogSignalType.DOWNSTREAM_TIMEOUT,
                        ApplicationLogSignalType.OUT_OF_MEMORY
                );
    }
}
