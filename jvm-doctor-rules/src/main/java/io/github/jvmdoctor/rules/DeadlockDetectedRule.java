package io.github.jvmdoctor.rules;

import io.github.jvmdoctor.domain.AnalysisContext;
import io.github.jvmdoctor.domain.ApplicationLogSignalType;
import io.github.jvmdoctor.domain.Finding;
import io.github.jvmdoctor.domain.Severity;

import java.util.List;

public final class DeadlockDetectedRule implements FindingRule {

    @Override
    public String id() {
        return "DEADLOCK_DETECTED";
    }

    @Override
    public List<Finding> evaluate(AnalysisContext context) {
        boolean threadDumpHit = RuleSupport.hasDeadlock(context);
        var logSignal = RuleSupport.firstLogSignal(context, ApplicationLogSignalType.DEADLOCK);
        if (!threadDumpHit && logSignal.isEmpty()) {
            return List.of();
        }

        return List.of(new Finding(
                id(),
                Severity.CRITICAL,
                threadDumpHit ? 0.98 : 0.76,
                "A Java-level deadlock or a very strong lock cycle signal was detected and should be handled first.",
                RuleSupport.combineEvidence(
                        threadDumpHit ? RuleSupport.evidenceFromThread("thread-dump", "Found one Java-level deadlock") : List.of(),
                        logSignal.map(RuleSupport::evidenceFromLogSignal).orElse(List.of())
                ),
                RuleSupport.defaultNextActions(
                        "Identify the blocked threads and lock owners.",
                        "Review lock acquisition order in the involved code path.",
                        "If required, isolate or restart the affected instance."
                )
        ));
    }
}
