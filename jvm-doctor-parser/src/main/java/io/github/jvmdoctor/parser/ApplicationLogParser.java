package io.github.jvmdoctor.parser;

import io.github.jvmdoctor.domain.ApplicationLogSignal;
import io.github.jvmdoctor.domain.ApplicationLogSignalType;
import io.github.jvmdoctor.domain.ApplicationLogSnapshot;
import io.github.jvmdoctor.domain.Artifact;
import io.github.jvmdoctor.domain.ArtifactType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ApplicationLogParser implements ArtifactParser<ApplicationLogSnapshot> {

    private static final Map<ApplicationLogSignalType, List<String>> KEYWORDS = Map.of(
            ApplicationLogSignalType.OUT_OF_MEMORY, List.of("outofmemoryerror"),
            ApplicationLogSignalType.GC_OVERHEAD, List.of("gc overhead limit exceeded"),
            ApplicationLogSignalType.DB_CONNECTION_TIMEOUT, List.of("connection is not available", "hikaripool", "connection timeout"),
            ApplicationLogSignalType.SQL_TIMEOUT, List.of("sqltimeout", "query timeout", "statement timeout"),
            ApplicationLogSignalType.DOWNSTREAM_TIMEOUT, List.of("read timed out", "connect timed out", "sockettimeoutexception"),
            ApplicationLogSignalType.DEADLOCK, List.of("deadlock"),
            ApplicationLogSignalType.REJECTED_EXECUTION, List.of("rejectedexecutionexception"),
            ApplicationLogSignalType.LOGGING_OVERHEAD, List.of("asyncappender", "outputstreamappender", "patternlayout")
    );

    @Override
    public boolean supports(ArtifactType artifactType) {
        return artifactType == ArtifactType.APPLICATION_LOG;
    }

    @Override
    public ApplicationLogSnapshot parse(Artifact artifact) {
        try {
            List<String> lines = Files.readAllLines(artifact.path(), StandardCharsets.UTF_8);
            Map<ApplicationLogSignalType, Integer> counts = new EnumMap<>(ApplicationLogSignalType.class);
            Map<ApplicationLogSignalType, String> samples = new EnumMap<>(ApplicationLogSignalType.class);

            for (String line : lines) {
                String normalized = line.toLowerCase(Locale.ROOT);
                for (Map.Entry<ApplicationLogSignalType, List<String>> entry : KEYWORDS.entrySet()) {
                    if (entry.getValue().stream().anyMatch(normalized::contains)) {
                        counts.merge(entry.getKey(), 1, Integer::sum);
                        samples.putIfAbsent(entry.getKey(), line.strip());
                    }
                }
            }

            List<ApplicationLogSignal> signals = new ArrayList<>();
            counts.forEach((type, count) -> signals.add(new ApplicationLogSignal(type, count, samples.get(type))));
            return new ApplicationLogSnapshot(lines.size(), signals);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read application log: " + artifact.path(), exception);
        }
    }
}
