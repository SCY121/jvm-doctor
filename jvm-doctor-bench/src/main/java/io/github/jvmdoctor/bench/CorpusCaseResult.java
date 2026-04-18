package io.github.jvmdoctor.bench;

import java.util.List;
import java.util.Objects;

public record CorpusCaseResult(
        String caseId,
        String name,
        List<String> actualFindingIds,
        List<String> failures
) {

    public CorpusCaseResult {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        actualFindingIds = List.copyOf(Objects.requireNonNull(actualFindingIds, "actualFindingIds must not be null"));
        failures = List.copyOf(Objects.requireNonNull(failures, "failures must not be null"));
    }

    public boolean passed() {
        return failures.isEmpty();
    }
}
