package io.github.jvmdoctor.rules;

import io.github.jvmdoctor.domain.AnalysisContext;
import io.github.jvmdoctor.domain.Finding;
import io.github.jvmdoctor.domain.Severity;

import java.util.List;
import java.util.Optional;

public final class CpuHotButNotGcRule implements FindingRule {

    @Override
    public String id() {
        return "CPU_HOT_BUT_NOT_GC";
    }

    @Override
    public List<Finding> evaluate(AnalysisContext context) {
        Optional<Double> systemCpu = RuleSupport.metricValue(context, "system.cpu.usage");
        Optional<Double> processCpu = RuleSupport.metricValue(context, "process.cpu.usage");
        Optional<Double> cpu = systemCpu.isPresent() ? systemCpu : processCpu;
        String cpuMetricName = systemCpu.isPresent() ? "system.cpu.usage" : "process.cpu.usage";
        var gcPause = RuleSupport.metricValue(context, "jvm.gc.pause");

        if (cpu.isEmpty() || cpu.get() < 0.80) {
            return List.of();
        }
        if (gcPause.isPresent() && gcPause.get() > 1_000) {
            return List.of();
        }

        return List.of(new Finding(
                id(),
                Severity.WARNING,
                0.68,
                "CPU usage is high, but GC does not appear to be the primary driver. The hotspot is more likely business logic, lock contention, or busy waiting.",
                RuleSupport.evidenceFromMetric(cpuMetricName, cpu.orElse(0.0)),
                RuleSupport.defaultNextActions(
                        "Capture async-profiler or JFR.",
                        "Inspect hot methods and lock contention."
                )
        ));
    }
}
