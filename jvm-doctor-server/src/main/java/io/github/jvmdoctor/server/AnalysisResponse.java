package io.github.jvmdoctor.server;

import io.github.jvmdoctor.domain.AnalysisOverview;
import io.github.jvmdoctor.domain.IncidentReport;

public record AnalysisResponse(
        String analysisId,
        String status,
        AnalysisOverview overview,
        IncidentReport report,
        AiAugmentation ai
) {
}
