package io.github.jvmdoctor.ai;

import io.github.jvmdoctor.domain.AnalysisContext;
import io.github.jvmdoctor.domain.Finding;
import io.github.jvmdoctor.domain.Hypothesis;

import java.util.List;

public interface HypothesisGenerator {

    List<Hypothesis> generate(AnalysisContext context, List<Finding> findings);
}

