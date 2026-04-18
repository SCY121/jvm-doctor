package io.github.jvmdoctor.rules;

import io.github.jvmdoctor.domain.AnalysisContext;
import io.github.jvmdoctor.domain.Finding;

import java.util.List;

public interface FindingRule {

    String id();

    List<Finding> evaluate(AnalysisContext context);
}
