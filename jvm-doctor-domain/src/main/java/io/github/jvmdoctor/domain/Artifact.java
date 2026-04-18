package io.github.jvmdoctor.domain;

import java.nio.file.Path;
import java.util.Objects;

public record Artifact(ArtifactType type, String source, Path path) {

    public Artifact {
        Objects.requireNonNull(type, "type 不能为空");
        Objects.requireNonNull(source, "source 不能为空");
        Objects.requireNonNull(path, "path 不能为空");
    }
}

