package io.github.jvmdoctor.rules;

import java.util.List;
import java.util.Objects;

public record RuleRegistry(List<FindingRule> rules) {

    public RuleRegistry {
        rules = List.copyOf(Objects.requireNonNull(rules, "rules must not be null"));
    }

    public static RuleRegistry defaults() {
        return new RuleRegistry(List.of(
                new HttpThreadPoolExhaustedRule(),
                new DbPoolExhaustedRule(),
                new DeadlockDetectedRule(),
                new LongBlockingIoRule(),
                new FullGcStormRule(),
                new OomOrLeakSuspectedRule(),
                new CpuHotButNotGcRule(),
                new LoggingOverheadSuspectedRule()
        ));
    }
}
