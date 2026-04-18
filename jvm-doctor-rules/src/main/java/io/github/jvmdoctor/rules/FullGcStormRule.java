package io.github.jvmdoctor.rules;

import io.github.jvmdoctor.domain.AnalysisContext;
import io.github.jvmdoctor.domain.ApplicationLogSignalType;
import io.github.jvmdoctor.domain.Finding;
import io.github.jvmdoctor.domain.Severity;

import java.util.List;

public final class FullGcStormRule implements FindingRule {

    @Override
    public String id() {
        return "FULL_GC_STORM";
    }

    @Override
    public List<Finding> evaluate(AnalysisContext context) {
        var gcPause = RuleSupport.metricValue(context, "jvm.gc.pause");
        var gcOverhead = RuleSupport.firstLogSignal(context, ApplicationLogSignalType.GC_OVERHEAD);
        boolean metricHit = gcPause.isPresent() && gcPause.get() >= 1_000;
        if (!metricHit && gcOverhead.isEmpty()) {
            return List.of();
        }

        return List.of(new Finding(
                id(),
                Severity.HIGH,
                metricHit ? 0.82 : 0.78,
                "GC pause time is elevated and the process may be in a full-GC storm or ineffective collection cycle.",
                RuleSupport.combineEvidence(
                        metricHit ? RuleSupport.evidenceFromMetric("jvm.gc.pause", gcPause.orElse(0.0)) : List.of(),
                        gcOverhead.map(RuleSupport::evidenceFromLogSignal).orElse(List.of())
                ),
                RuleSupport.defaultNextActions(
                        "Inspect heap usage trends.",
                        "Check for large objects or cache growth.",
                        "Capture JFR or GC logs if more evidence is needed."
                )
        ));
    }
}
