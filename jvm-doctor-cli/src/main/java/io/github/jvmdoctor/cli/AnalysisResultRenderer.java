package io.github.jvmdoctor.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.jvmdoctor.domain.AnalysisResult;

public final class AnalysisResultRenderer {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public String render(AnalysisResult result, ReportFormat format) {
        return switch (format) {
            case json -> renderJson(result);
            case markdown -> renderMarkdown(result);
        };
    }

    private String renderJson(AnalysisResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to render JSON report", exception);
        }
    }

    private String renderMarkdown(AnalysisResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("# jvm-doctor Analysis Report\n\n");
        builder.append("## Overview\n\n");
        builder.append("- Input artifacts: ").append(result.overview().artifactCount()).append('\n');
        builder.append("- Thread dump files: ").append(result.overview().threadDumpCount()).append('\n');
        builder.append("- Total parsed threads: ").append(result.overview().totalThreadCount()).append('\n');
        builder.append("- Actuator metrics files: ").append(result.overview().actuatorMetricsFileCount()).append('\n');
        builder.append("- Available metric names: ").append(result.overview().availableMetricNameCount()).append('\n');
        builder.append("- Detailed metric series: ").append(result.overview().detailedMetricCount()).append('\n');
        builder.append("- Application log files: ").append(result.overview().applicationLogFileCount()).append('\n');
        builder.append("- Detected log signals: ").append(result.overview().detectedLogSignalCount()).append('\n');
        builder.append('\n');
        builder.append("## Summary\n\n");
        builder.append(result.report().executiveSummary()).append("\n\n");
        builder.append("## Findings\n\n");
        if (result.report().findings().isEmpty()) {
            builder.append("- No high-confidence findings matched.\n");
        } else {
            result.report().findings().forEach(finding -> builder.append("- [")
                    .append(finding.severity())
                    .append("] ")
                    .append(finding.summary())
                    .append(" (")
                    .append(finding.ruleId())
                    .append(", confidence=")
                    .append(String.format("%.2f", finding.confidence()))
                    .append(")\n"));
        }
        builder.append('\n');
        builder.append("## Top Hypotheses\n\n");
        result.report().topHypotheses().forEach(hypothesis -> builder.append("- ")
                .append(hypothesis.title())
                .append(" (score=")
                .append(String.format("%.2f", hypothesis.score()))
                .append(")\n"));
        builder.append('\n');
        builder.append("## Thread States\n\n");
        if (result.overview().threadStateCounts().isEmpty()) {
            builder.append("- No thread state data was parsed.\n");
        } else {
            result.overview().threadStateCounts().forEach((state, count) ->
                    builder.append("- ").append(state).append(": ").append(count).append('\n'));
        }
        builder.append('\n');
        builder.append("## Next Actions\n\n");
        result.report().recommendedActions().forEach(action ->
                builder.append("- ").append(action).append('\n'));
        return builder.toString();
    }
}
