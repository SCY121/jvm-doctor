package io.github.jvmdoctor.rules;

import io.github.jvmdoctor.domain.AnalysisContext;
import io.github.jvmdoctor.domain.ApplicationLogSignalType;
import io.github.jvmdoctor.domain.Finding;
import io.github.jvmdoctor.domain.Severity;

import java.util.List;

public final class DbPoolExhaustedRule implements FindingRule {

    @Override
    public String id() {
        return "DB_POOL_EXHAUSTED";
    }

    @Override
    public List<Finding> evaluate(AnalysisContext context) {
        var active = RuleSupport.metricValue(context, "hikaricp.connections.active");
        var max = RuleSupport.metricValue(context, "hikaricp.connections.max");
        var timeoutSignal = RuleSupport.firstLogSignal(context, ApplicationLogSignalType.DB_CONNECTION_TIMEOUT);
        var sqlTimeoutSignal = RuleSupport.firstLogSignal(context, ApplicationLogSignalType.SQL_TIMEOUT);

        boolean metricHit = active.isPresent() && max.isPresent() && max.get() > 0 && active.get() / max.get() >= 0.9;
        boolean logHit = timeoutSignal.isPresent() || sqlTimeoutSignal.isPresent();

        if (!metricHit && !logHit) {
            return List.of();
        }

        return List.of(new Finding(
                id(),
                Severity.CRITICAL,
                metricHit ? 0.93 : 0.80,
                "The database connection pool is exhausted or nearly exhausted, so request threads are likely blocked waiting for connections.",
                RuleSupport.combineEvidence(
                        metricHit ? RuleSupport.evidenceFromMetric("hikaricp.connections.active", active.orElse(0.0)) : List.of(),
                        metricHit ? RuleSupport.evidenceFromMetric("hikaricp.connections.max", max.orElse(0.0)) : List.of(),
                        timeoutSignal.map(RuleSupport::evidenceFromLogSignal).orElse(List.of()),
                        sqlTimeoutSignal.map(RuleSupport::evidenceFromLogSignal).orElse(List.of())
                ),
                RuleSupport.defaultNextActions(
                        "Investigate slow SQL first.",
                        "Review transaction boundaries for long-lived transactions.",
                        "Confirm connections are always returned to the pool."
                )
        ));
    }
}
