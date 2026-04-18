package io.github.jvmdoctor.ai;

import io.github.jvmdoctor.domain.AnalysisContext;
import io.github.jvmdoctor.domain.Finding;
import io.github.jvmdoctor.domain.Hypothesis;
import io.github.jvmdoctor.domain.Severity;

import java.util.Comparator;
import java.util.List;

public final class RuleBasedHypothesisGenerator implements HypothesisGenerator {

    @Override
    public List<Hypothesis> generate(AnalysisContext context, List<Finding> findings) {
        if (findings.isEmpty()) {
            return List.of(new Hypothesis(
                    "No high-confidence root cause yet",
                    0.30,
                    "The current evidence set was parsed successfully, but no high-confidence rule matched. "
                            + "Add logs, more metrics, or a more complete thread dump.",
                    List.of(),
                    List.of("Application logs", "Additional Actuator metrics", "Multiple thread dumps")
            ));
        }

        return findings.stream()
                .sorted(Comparator
                        .comparingInt((Finding finding) -> severityWeight(finding.severity()))
                        .reversed()
                        .thenComparing(Finding::confidence, Comparator.reverseOrder()))
                .limit(3)
                .map(finding -> new Hypothesis(
                        finding.summary(),
                        finding.confidence(),
                        buildExplanation(finding, context),
                        List.of(finding.ruleId()),
                        List.of()
                ))
                .toList();
    }

    private String buildExplanation(Finding finding, AnalysisContext context) {
        return "Built from "
                + context.threadDumps().size()
                + " thread dump file(s), "
                + context.actuatorMetrics().size()
                + " actuator metrics snapshot(s), and "
                + context.applicationLogs().size()
                + " application log file(s); matched rule "
                + finding.ruleId()
                + ".";
    }

    private int severityWeight(Severity severity) {
        return switch (severity) {
            case CRITICAL -> 4;
            case HIGH -> 3;
            case WARNING -> 2;
            case INFO -> 1;
        };
    }
}
