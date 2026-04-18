package io.github.jvmdoctor.cli;

import io.github.jvmdoctor.domain.AnalysisContext;
import io.github.jvmdoctor.domain.AnalysisOverview;
import io.github.jvmdoctor.domain.AnalysisResult;
import io.github.jvmdoctor.domain.ApplicationLogSnapshot;
import io.github.jvmdoctor.domain.Hypothesis;
import io.github.jvmdoctor.domain.IncidentInput;
import io.github.jvmdoctor.domain.IncidentReport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisResultRendererTest {

    @Test
    void shouldRenderMarkdownSummary() {
        AnalysisResult result = new AnalysisResult(
                new AnalysisContext(new IncidentInput(List.of(), Map.of()), List.of(), List.of(), List.of(new ApplicationLogSnapshot(2, List.of()))),
                new AnalysisOverview(3, 1, 3, Map.of("RUNNABLE", 1L), 1, 3, 2, 1, 0),
                new IncidentReport(
                        List.of(),
                        List.of(new Hypothesis("HTTP thread pool exhaustion", 0.80, "Test explanation", List.of("RULE"), List.of())),
                        "Parsed sample inputs.",
                        List.of("Inspect the rule matches.")
                )
        );

        String markdown = new AnalysisResultRenderer().render(result, ReportFormat.markdown);

        assertThat(markdown).contains("# jvm-doctor Analysis Report");
        assertThat(markdown).contains("Parsed sample inputs.");
        assertThat(markdown).contains("RUNNABLE: 1");
        assertThat(markdown).contains("HTTP thread pool exhaustion");
    }
}
