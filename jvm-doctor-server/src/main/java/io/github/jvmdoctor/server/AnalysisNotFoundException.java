package io.github.jvmdoctor.server;

public final class AnalysisNotFoundException extends RuntimeException {

    public AnalysisNotFoundException(String analysisId) {
        super("Analysis not found: " + analysisId);
    }
}
