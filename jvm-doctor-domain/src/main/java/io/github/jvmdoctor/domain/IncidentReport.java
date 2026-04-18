package io.github.jvmdoctor.domain;

import java.util.List;
import java.util.Objects;

public record IncidentReport(
        List<Finding> findings,
        List<Hypothesis> topHypotheses,
        String executiveSummary,
        List<String> recommendedActions
) {

    public IncidentReport {
        findings = List.copyOf(Objects.requireNonNull(findings, "findings 不能为空"));
        topHypotheses = List.copyOf(Objects.requireNonNull(topHypotheses, "topHypotheses 不能为空"));
        Objects.requireNonNull(executiveSummary, "executiveSummary 不能为空");
        recommendedActions = List.copyOf(Objects.requireNonNull(recommendedActions, "recommendedActions 不能为空"));
    }
}

