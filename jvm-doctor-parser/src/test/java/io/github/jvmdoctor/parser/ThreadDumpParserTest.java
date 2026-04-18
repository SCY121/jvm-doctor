package io.github.jvmdoctor.parser;

import io.github.jvmdoctor.domain.Artifact;
import io.github.jvmdoctor.domain.ArtifactType;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadDumpParserTest {

    private final ThreadDumpParser parser = new ThreadDumpParser();

    @Test
    void shouldParseThreadNamesAndStates() {
        Artifact artifact = new Artifact(
                ArtifactType.THREAD_DUMP,
                "sample-thread-dump",
                Path.of("src/test/resources/thread-dump-sample.txt")
        );

        var snapshot = parser.parse(artifact);

        assertThat(snapshot.threadCount()).isEqualTo(3);
        assertThat(snapshot.stateCounts()).containsEntry("RUNNABLE", 1L);
        assertThat(snapshot.stateCounts()).containsEntry("WAITING", 1L);
        assertThat(snapshot.stateCounts()).containsEntry("TIMED_WAITING", 1L);
        assertThat(snapshot.threads())
                .extracting(thread -> thread.name())
                .contains("http-nio-8080-exec-1", "http-nio-8080-exec-2", "HikariPool-1 housekeeper");
    }
}

