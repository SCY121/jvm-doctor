package io.github.jvmdoctor.rules;

import io.github.jvmdoctor.domain.AnalysisContext;
import io.github.jvmdoctor.domain.ApplicationLogSignalType;
import io.github.jvmdoctor.domain.Finding;
import io.github.jvmdoctor.domain.Severity;

import java.util.List;

public final class LoggingOverheadSuspectedRule implements FindingRule {

    private static final List<String> LOGGING_KEYWORDS = List.of(
            "logback",
            "outputstreamappender",
            "asyncappender",
            "patternlayout"
    );

    @Override
    public String id() {
        return "LOGGING_OVERHEAD_SUSPECTED";
    }

    @Override
    public List<Finding> evaluate(AnalysisContext context) {
        long stackMatches = RuleSupport.countThreadsWithStackKeyword(context, LOGGING_KEYWORDS);
        var signal = RuleSupport.firstLogSignal(context, ApplicationLogSignalType.LOGGING_OVERHEAD);
        if (stackMatches < 2 && signal.isEmpty()) {
            return List.of();
        }

        return List.of(new Finding(
                id(),
                Severity.WARNING,
                signal.isPresent() ? 0.72 : 0.60,
                "The logging path may be adding noticeable overhead through synchronous appenders or excessive log volume.",
                RuleSupport.combineEvidence(
                        RuleSupport.evidenceFromThread("thread-dump", "threadsInLoggingPath=" + stackMatches),
                        signal.map(RuleSupport::evidenceFromLogSignal).orElse(List.of())
                ),
                RuleSupport.defaultNextActions(
                        "Review log level settings.",
                        "Check whether appenders are synchronous.",
                        "Assess log volume and disk write pressure."
                )
        ));
    }
}
