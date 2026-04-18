package io.github.jvmdoctor.parser;

import io.github.jvmdoctor.domain.Artifact;
import io.github.jvmdoctor.domain.ArtifactType;
import io.github.jvmdoctor.domain.ThreadDumpSnapshot;
import io.github.jvmdoctor.domain.ThreadDumpThread;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ThreadDumpParser implements ArtifactParser<ThreadDumpSnapshot> {

    private static final Pattern THREAD_NAME_PATTERN = Pattern.compile("^\"([^\"]+)\".*$");

    @Override
    public boolean supports(ArtifactType artifactType) {
        return artifactType == ArtifactType.THREAD_DUMP || artifactType == ArtifactType.ACTUATOR_THREAD_DUMP;
    }

    @Override
    public ThreadDumpSnapshot parse(Artifact artifact) {
        try {
            List<String> lines = Files.readAllLines(artifact.path(), StandardCharsets.UTF_8);
            List<ThreadDumpThread> threads = parseThreads(lines);
            Map<String, Long> stateCounts = threads.stream()
                    .collect(Collectors.groupingBy(ThreadDumpThread::state, Collectors.counting()));
            boolean deadlockDetected = lines.stream().anyMatch(line -> line.contains("Found one Java-level deadlock"));
            return new ThreadDumpSnapshot(threads.size(), deadlockDetected, stateCounts, threads);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read thread dump: " + artifact.path(), exception);
        }
    }

    private List<ThreadDumpThread> parseThreads(List<String> lines) {
        List<ThreadDumpThread> threads = new ArrayList<>();
        List<String> currentBlock = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.stripTrailing();
            if (line.startsWith("\"")) {
                if (!currentBlock.isEmpty()) {
                    threads.add(buildThread(currentBlock));
                }
                currentBlock = new ArrayList<>();
            }

            if (!currentBlock.isEmpty() || line.startsWith("\"")) {
                currentBlock.add(line);
            }
        }

        if (!currentBlock.isEmpty()) {
            threads.add(buildThread(currentBlock));
        }

        return threads;
    }

    private ThreadDumpThread buildThread(List<String> block) {
        String header = block.getFirst();
        Matcher matcher = THREAD_NAME_PATTERN.matcher(header);
        String name = matcher.matches() ? matcher.group(1) : header;
        boolean daemon = header.contains(" daemon ");
        String state = block.stream()
                .map(String::trim)
                .filter(line -> line.startsWith("java.lang.Thread.State:"))
                .map(line -> line.substring("java.lang.Thread.State:".length()).trim())
                .map(this::normalizeState)
                .findFirst()
                .orElse("UNKNOWN");
        List<String> stackLines = block.stream()
                .map(String::trim)
                .filter(line -> line.startsWith("at ") || line.startsWith("- "))
                .toList();
        return new ThreadDumpThread(name, state, daemon, stackLines);
    }

    private String normalizeState(String rawState) {
        String normalized = rawState.trim();
        int blankIndex = normalized.indexOf(' ');
        int bracketIndex = normalized.indexOf('(');
        int endIndex = normalized.length();
        if (blankIndex > -1) {
            endIndex = Math.min(endIndex, blankIndex);
        }
        if (bracketIndex > -1) {
            endIndex = Math.min(endIndex, bracketIndex);
        }
        return normalized.substring(0, endIndex).toUpperCase();
    }
}
