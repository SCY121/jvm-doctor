package io.github.jvmdoctor.domain;

import java.util.List;
import java.util.Objects;

public record Hypothesis(
        String title,
        double score,
        String explanation,
        List<String> supportingFindingIds,
        List<String> missingData
) {

    public Hypothesis {
        Objects.requireNonNull(title, "title 不能为空");
        Objects.requireNonNull(explanation, "explanation 不能为空");
        supportingFindingIds = List.copyOf(Objects.requireNonNull(supportingFindingIds, "supportingFindingIds 不能为空"));
        missingData = List.copyOf(Objects.requireNonNull(missingData, "missingData 不能为空"));
    }
}

