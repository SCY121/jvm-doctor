package io.github.jvmdoctor.bench;

import java.util.List;
import java.util.Objects;

public record CorpusBenchmarkReport(List<CorpusCaseResult> cases) {

    public CorpusBenchmarkReport {
        cases = List.copyOf(Objects.requireNonNull(cases, "cases must not be null"));
    }

    public int totalCases() {
        return cases.size();
    }

    public List<CorpusCaseResult> passedCases() {
        return cases.stream().filter(CorpusCaseResult::passed).toList();
    }

    public List<CorpusCaseResult> failedCases() {
        return cases.stream().filter(result -> !result.passed()).toList();
    }

    public String renderConsoleSummary() {
        StringBuilder builder = new StringBuilder();
        builder.append("jvm-doctor benchmark\n");
        builder.append("Corpus cases: ").append(totalCases()).append('\n');
        builder.append("Passed: ").append(passedCases().size()).append('\n');
        builder.append("Failed: ").append(failedCases().size()).append('\n');
        builder.append('\n');

        for (CorpusCaseResult result : cases) {
            builder.append(result.passed() ? "[PASS] " : "[FAIL] ")
                    .append(result.caseId())
                    .append(" - ")
                    .append(result.name())
                    .append('\n');
            builder.append("  Findings: ").append(String.join(", ", result.actualFindingIds())).append('\n');
            for (String failure : result.failures()) {
                builder.append("  ").append(failure).append('\n');
            }
        }

        return builder.toString().stripTrailing();
    }
}
