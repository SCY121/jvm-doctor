package io.github.jvmdoctor.bench;

import java.util.List;

record CorpusCaseExpectation(
        String name,
        List<String> requiredFindingIds,
        List<String> forbiddenFindingIds,
        Integer maxFindingCount
) {

    CorpusCaseExpectation {
        name = name == null || name.isBlank() ? "Unnamed case" : name;
        requiredFindingIds = requiredFindingIds == null ? List.of() : List.copyOf(requiredFindingIds);
        forbiddenFindingIds = forbiddenFindingIds == null ? List.of() : List.copyOf(forbiddenFindingIds);
    }
}
