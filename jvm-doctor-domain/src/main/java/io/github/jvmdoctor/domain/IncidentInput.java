package io.github.jvmdoctor.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record IncidentInput(List<Artifact> artifacts, Map<String, String> metadata) {

    public IncidentInput {
        artifacts = List.copyOf(Objects.requireNonNull(artifacts, "artifacts 不能为空"));
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata 不能为空"));
    }
}

