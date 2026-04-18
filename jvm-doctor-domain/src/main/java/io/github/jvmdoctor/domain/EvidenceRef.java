package io.github.jvmdoctor.domain;

import java.util.Objects;

public record EvidenceRef(String artifactSource, String locator, String summary) {

    public EvidenceRef {
        Objects.requireNonNull(artifactSource, "artifactSource 不能为空");
        Objects.requireNonNull(locator, "locator 不能为空");
        Objects.requireNonNull(summary, "summary 不能为空");
    }
}

