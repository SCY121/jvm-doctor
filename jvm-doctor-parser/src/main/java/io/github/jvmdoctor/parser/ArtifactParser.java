package io.github.jvmdoctor.parser;

import io.github.jvmdoctor.domain.Artifact;
import io.github.jvmdoctor.domain.ArtifactType;

public interface ArtifactParser<T> {

    boolean supports(ArtifactType artifactType);

    T parse(Artifact artifact);
}

