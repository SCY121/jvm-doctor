package io.github.jvmdoctor.rules;

import io.github.jvmdoctor.domain.AnalysisContext;
import io.github.jvmdoctor.domain.ApplicationLogSignalType;
import io.github.jvmdoctor.domain.Finding;
import io.github.jvmdoctor.domain.Severity;

import java.util.List;

public final class OomOrLeakSuspectedRule implements FindingRule {

    @Override
    public String id() {
        return "OOM_OR_LEAK_SUSPECTED";
    }

    @Override
    public List<Finding> evaluate(AnalysisContext context) {
        var oom = RuleSupport.firstLogSignal(context, ApplicationLogSignalType.OUT_OF_MEMORY);
        var gcOverhead = RuleSupport.firstLogSignal(context, ApplicationLogSignalType.GC_OVERHEAD);
        if (oom.isEmpty() && gcOverhead.isEmpty()) {
            return List.of();
        }

        return List.of(new Finding(
                id(),
                Severity.CRITICAL,
                oom.isPresent() ? 0.95 : 0.82,
                "The logs contain OOM or GC-overhead signals, which strongly suggests memory pressure, a leak, or insufficient heap sizing.",
                RuleSupport.combineEvidence(
                        oom.map(RuleSupport::evidenceFromLogSignal).orElse(List.of()),
                        gcOverhead.map(RuleSupport::evidenceFromLogSignal).orElse(List.of())
                ),
                RuleSupport.defaultNextActions(
                        "Capture a heap dump.",
                        "Use JFR to inspect allocation hotspots.",
                        "Review cache growth and bulk-loading paths."
                )
        ));
    }
}
