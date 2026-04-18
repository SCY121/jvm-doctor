package io.github.jvmdoctor.domain;

import java.util.Objects;

public record ApplicationLogSignal(
        ApplicationLogSignalType type,
        int count,
        String sampleLine
) {

    public ApplicationLogSignal {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(sampleLine, "sampleLine must not be null");
    }
}
