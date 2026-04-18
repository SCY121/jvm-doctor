package io.github.jvmdoctor.domain;

import java.util.List;
import java.util.Objects;

public record Finding(
        String ruleId,
        Severity severity,
        double confidence,
        String summary,
        List<EvidenceRef> evidenceRefs,
        List<String> nextActions
) {

    public Finding {
        Objects.requireNonNull(ruleId, "ruleId 不能为空");
        Objects.requireNonNull(severity, "severity 不能为空");
        Objects.requireNonNull(summary, "summary 不能为空");
        evidenceRefs = List.copyOf(Objects.requireNonNull(evidenceRefs, "evidenceRefs 不能为空"));
        nextActions = List.copyOf(Objects.requireNonNull(nextActions, "nextActions 不能为空"));
    }
}

