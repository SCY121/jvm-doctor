package io.github.jvmdoctor.bench;

import io.github.jvmdoctor.engine.IncidentAnalysisEngine;

import java.nio.file.Path;

public final class BenchApplication {

    private BenchApplication() {
    }

    public static void main(String[] args) {
        Path corpusRoot = args.length > 0
                ? Path.of(args[0]).toAbsolutePath().normalize()
                : Path.of("samples", "incidents").toAbsolutePath().normalize();

        CorpusBenchmarkReport report = new CorpusBenchmarkRunner(IncidentAnalysisEngine.bootstrap()).run(corpusRoot);
        System.out.println(report.renderConsoleSummary());

        if (!report.failedCases().isEmpty()) {
            System.exit(1);
        }
    }
}
