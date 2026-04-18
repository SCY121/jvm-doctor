package io.github.jvmdoctor.server;

import io.github.jvmdoctor.domain.AnalysisResult;

public interface AiAugmentationService {

    AiAugmentation augment(AnalysisResult analysisResult);
}
