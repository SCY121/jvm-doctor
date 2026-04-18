package io.github.jvmdoctor.domain;

import java.util.Objects;

public record AnalysisResult(
        AnalysisContext context,
        AnalysisOverview overview,
        IncidentReport report
) {

    public AnalysisResult {
        Objects.requireNonNull(context, "context 不能为空");
        Objects.requireNonNull(overview, "overview 不能为空");
        Objects.requireNonNull(report, "report 不能为空");
    }
}
