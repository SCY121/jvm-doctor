package io.github.jvmdoctor.rules;

import io.github.jvmdoctor.domain.AnalysisContext;
import io.github.jvmdoctor.domain.Artifact;
import io.github.jvmdoctor.domain.ArtifactType;
import io.github.jvmdoctor.domain.IncidentInput;
import io.github.jvmdoctor.parser.ActuatorMetricsParser;
import io.github.jvmdoctor.parser.ApplicationLogParser;
import io.github.jvmdoctor.parser.ThreadDumpParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DbPoolExhaustedRuleTest {

    @Test
    void shouldProduceDbPoolFinding() {
        Artifact threadDump = new Artifact(ArtifactType.THREAD_DUMP, "thread", Path.of("src/test/resources/db-pool-thread-dump.txt"));
        Artifact metrics = new Artifact(ArtifactType.ACTUATOR_METRICS, "metrics", Path.of("src/test/resources/db-pool-metrics.json"));
        Artifact log = new Artifact(ArtifactType.APPLICATION_LOG, "log", Path.of("src/test/resources/db-pool.log"));
        IncidentInput input = new IncidentInput(
                List.of(threadDump, metrics, log),
                Map.of("case", "db-pool")
        );
        AnalysisContext context = new AnalysisContext(
                input,
                List.of(new ThreadDumpParser().parse(threadDump)),
                List.of(new ActuatorMetricsParser().parse(metrics)),
                List.of(new ApplicationLogParser().parse(log))
        );

        List<String> ruleIds = RuleRegistry.defaults().rules().stream()
                .flatMap(rule -> rule.evaluate(context).stream())
                .map(finding -> finding.ruleId())
                .toList();

        assertThat(ruleIds).contains("DB_POOL_EXHAUSTED", "LONG_BLOCKING_IO", "HTTP_THREAD_POOL_EXHAUSTED");
    }
}
