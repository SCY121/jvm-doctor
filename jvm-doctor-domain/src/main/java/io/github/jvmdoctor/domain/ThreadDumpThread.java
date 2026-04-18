package io.github.jvmdoctor.domain;

import java.util.List;
import java.util.Objects;

public record ThreadDumpThread(
        String name,
        String state,
        boolean daemon,
        List<String> stackLines
) {

    public ThreadDumpThread {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(state, "state must not be null");
        stackLines = List.copyOf(Objects.requireNonNull(stackLines, "stackLines must not be null"));
    }
}
