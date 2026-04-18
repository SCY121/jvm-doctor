package io.github.jvmdoctor.rules;

import io.github.jvmdoctor.domain.AnalysisContext;
import io.github.jvmdoctor.domain.ApplicationLogSignalType;
import io.github.jvmdoctor.domain.Finding;
import io.github.jvmdoctor.domain.Severity;

import java.util.List;

public final class LongBlockingIoRule implements FindingRule {

    private static final List<String> IO_KEYWORDS = List.of(
            "socketinputstream",
            "socketdispatcher",
            "niosocketimpl",
            "okhttp",
            "apache.http",
            "feign"
    );

    @Override
    public String id() {
        return "LONG_BLOCKING_IO";
    }

    @Override
    public List<Finding> evaluate(AnalysisContext context) {
        long threadMatches = RuleSupport.countThreadsWithStackKeyword(context, IO_KEYWORDS);
        var timeoutSignal = RuleSupport.firstLogSignal(context, ApplicationLogSignalType.DOWNSTREAM_TIMEOUT);
        if (threadMatches < 2 && timeoutSignal.isEmpty()) {
            return List.of();
        }

        return List.of(new Finding(
                id(),
                Severity.HIGH,
                timeoutSignal.isPresent() ? 0.84 : 0.70,
                "Thread stacks and logs both point to downstream I/O blocking rather than a local CPU hotspot.",
                RuleSupport.combineEvidence(
                        RuleSupport.evidenceFromThread("thread-dump", "threadsWithBlockingIo=" + threadMatches),
                        timeoutSignal.map(RuleSupport::evidenceFromLogSignal).orElse(List.of())
                ),
                RuleSupport.defaultNextActions(
                        "Check downstream latency and timeout settings.",
                        "Look for retry storms or connection starvation.",
                        "Confirm that worker pools are not being consumed by remote calls."
                )
        ));
    }
}
