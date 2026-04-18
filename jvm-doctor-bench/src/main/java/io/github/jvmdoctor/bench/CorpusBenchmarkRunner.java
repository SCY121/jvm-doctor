package io.github.jvmdoctor.bench;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvmdoctor.domain.AnalysisResult;
import io.github.jvmdoctor.domain.Artifact;
import io.github.jvmdoctor.domain.ArtifactType;
import io.github.jvmdoctor.domain.IncidentInput;
import io.github.jvmdoctor.engine.IncidentAnalysisEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public final class CorpusBenchmarkRunner {

    private static final String EXPECTATIONS_FILE = "expectations.json";

    private final IncidentAnalysisEngine engine;
    private final ObjectMapper objectMapper;

    public CorpusBenchmarkRunner(IncidentAnalysisEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
        this.objectMapper = new ObjectMapper();
    }

    public CorpusBenchmarkReport run(Path corpusRoot) {
        if (!Files.isDirectory(corpusRoot)) {
            throw new IllegalArgumentException("Corpus root does not exist: " + corpusRoot);
        }

        List<CorpusCaseResult> cases = new ArrayList<>();
        try (Stream<Path> directories = Files.list(corpusRoot)) {
            directories
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(caseDirectory -> {
                        Path expectationFile = caseDirectory.resolve(EXPECTATIONS_FILE);
                        if (Files.exists(expectationFile)) {
                            cases.add(runCase(caseDirectory, expectationFile));
                        }
                    });
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to scan corpus directory: " + corpusRoot, exception);
        }

        return new CorpusBenchmarkReport(cases);
    }

    private CorpusCaseResult runCase(Path caseDirectory, Path expectationFile) {
        CorpusCaseExpectation expectation = readExpectation(expectationFile);
        AnalysisResult result = engine.analyze(new IncidentInput(
                loadArtifacts(caseDirectory),
                Map.of(
                        "mode", "benchmark",
                        "caseId", caseDirectory.getFileName().toString()
                )
        ));

        Set<String> actualFindingIds = result.report().findings().stream()
                .map(finding -> finding.ruleId())
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        List<String> missingRequired = expectation.requiredFindingIds().stream()
                .filter(ruleId -> !actualFindingIds.contains(ruleId))
                .toList();
        List<String> unexpectedForbidden = expectation.forbiddenFindingIds().stream()
                .filter(actualFindingIds::contains)
                .toList();

        List<String> failures = new ArrayList<>();
        if (!missingRequired.isEmpty()) {
            failures.add("Missing required findings: " + String.join(", ", missingRequired));
        }
        if (!unexpectedForbidden.isEmpty()) {
            failures.add("Matched forbidden findings: " + String.join(", ", unexpectedForbidden));
        }
        if (expectation.maxFindingCount() != null && actualFindingIds.size() > expectation.maxFindingCount()) {
            failures.add("Finding count exceeded limit: actual=" + actualFindingIds.size()
                    + ", max=" + expectation.maxFindingCount());
        }

        return new CorpusCaseResult(
                caseDirectory.getFileName().toString(),
                expectation.name(),
                actualFindingIds.stream().toList(),
                failures
        );
    }

    private CorpusCaseExpectation readExpectation(Path expectationFile) {
        try {
            return objectMapper.readValue(expectationFile.toFile(), CorpusCaseExpectation.class);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read expectation file: " + expectationFile, exception);
        }
    }

    private List<Artifact> loadArtifacts(Path caseDirectory) {
        try (Stream<Path> files = Files.list(caseDirectory)) {
            return files
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(this::toArtifact)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read case directory: " + caseDirectory, exception);
        }
    }

    private Artifact toArtifact(Path file) {
        String fileName = file.getFileName().toString();
        ArtifactType type;
        if (fileName.startsWith("actuator-thread-dump")) {
            type = ArtifactType.ACTUATOR_THREAD_DUMP;
        } else if (fileName.startsWith("thread-dump")) {
            type = ArtifactType.THREAD_DUMP;
        } else if (fileName.startsWith("actuator-metrics")) {
            type = ArtifactType.ACTUATOR_METRICS;
        } else if (fileName.endsWith(".log")) {
            type = ArtifactType.APPLICATION_LOG;
        } else {
            type = null;
        }
        if (type == null) {
            return null;
        }
        return new Artifact(type, fileName, file.toAbsolutePath().normalize());
    }
}
