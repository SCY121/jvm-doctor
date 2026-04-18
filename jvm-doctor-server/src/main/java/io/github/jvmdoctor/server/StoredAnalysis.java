package io.github.jvmdoctor.server;

import io.github.jvmdoctor.domain.AnalysisResult;

import java.time.Instant;

record StoredAnalysis(
        String id,
        Instant createdAt,
        AnalysisResponse response
) {
}
