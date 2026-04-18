package io.github.jvmdoctor.domain;

import java.util.List;
import java.util.Objects;

public record ApplicationLogSnapshot(
        int lineCount,
        List<ApplicationLogSignal> signals
) {

    public ApplicationLogSnapshot {
        signals = List.copyOf(Objects.requireNonNull(signals, "signals must not be null"));
    }
}
