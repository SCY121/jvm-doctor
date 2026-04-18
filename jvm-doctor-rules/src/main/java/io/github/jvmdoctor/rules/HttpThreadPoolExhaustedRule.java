package io.github.jvmdoctor.rules;

import io.github.jvmdoctor.domain.AnalysisContext;
import io.github.jvmdoctor.domain.Finding;
import io.github.jvmdoctor.domain.Severity;

import java.util.List;

public final class HttpThreadPoolExhaustedRule implements FindingRule {

    private static final List<String> HTTP_PREFIXES = List.of("http-nio-", "http-bio-", "undertow", "tomcat-");
    private static final List<String> BUSY_STATES = List.of("RUNNABLE", "WAITING", "BLOCKED");

    @Override
    public String id() {
        return "HTTP_THREAD_POOL_EXHAUSTED";
    }

    @Override
    public List<Finding> evaluate(AnalysisContext context) {
        long httpThreads = RuleSupport.countThreadsByNamePrefix(context, HTTP_PREFIXES);
        long busyThreads = RuleSupport.countThreadsByStateAndPrefix(context, HTTP_PREFIXES, BUSY_STATES);
        var tomcatBusy = RuleSupport.metricValue(context, "tomcat.threads.busy");
        var tomcatCurrent = RuleSupport.metricValue(context, "tomcat.threads.current");

        boolean metricHit = tomcatBusy.isPresent() && tomcatCurrent.isPresent()
                && tomcatCurrent.get() > 0
                && tomcatBusy.get() / tomcatCurrent.get() >= 0.8;
        boolean threadHit = httpThreads >= 4 && busyThreads >= Math.max(3, Math.round(httpThreads * 0.75));

        if (!metricHit && !threadHit) {
            return List.of();
        }

        return List.of(new Finding(
                id(),
                Severity.HIGH,
                metricHit ? 0.88 : 0.72,
                "HTTP worker threads are close to exhaustion and request handling is likely backing up.",
                metricHit
                        ? RuleSupport.combineEvidence(
                        RuleSupport.evidenceFromMetric("tomcat.threads.busy", tomcatBusy.orElse(0.0)),
                        RuleSupport.evidenceFromMetric("tomcat.threads.current", tomcatCurrent.orElse(0.0))
                )
                        : RuleSupport.evidenceFromThread("http-thread-group", "busy=" + busyThreads + ", total=" + httpThreads),
                RuleSupport.defaultNextActions(
                        "Inspect the slowest requests first.",
                        "Check whether the HTTP worker pool is undersized.",
                        "Correlate with downstream blocking or database waits."
                )
        ));
    }
}
