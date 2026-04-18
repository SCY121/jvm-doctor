package io.github.jvmdoctor.rules;

import io.github.jvmdoctor.domain.ActuatorMetric;
import io.github.jvmdoctor.domain.ActuatorMetricsSnapshot;
import io.github.jvmdoctor.domain.AnalysisContext;
import io.github.jvmdoctor.domain.ApplicationLogSignal;
import io.github.jvmdoctor.domain.ApplicationLogSignalType;
import io.github.jvmdoctor.domain.EvidenceRef;
import io.github.jvmdoctor.domain.ThreadDumpSnapshot;
import io.github.jvmdoctor.domain.ThreadDumpThread;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

final class RuleSupport {

    private RuleSupport() {
    }

    static Optional<Double> metricValue(AnalysisContext context, String metricName) {
        return context.actuatorMetrics().stream()
                .map(ActuatorMetricsSnapshot::detailedMetrics)
                .flatMap(List::stream)
                .filter(metric -> metric.name().equals(metricName))
                .map(ActuatorMetric::measurements)
                .flatMap(List::stream)
                .map(measurement -> measurement.value())
                .findFirst();
    }

    static int logSignalCount(AnalysisContext context, ApplicationLogSignalType type) {
        return context.applicationLogs().stream()
                .map(snapshot -> snapshot.signals().stream()
                        .filter(signal -> signal.type() == type)
                        .mapToInt(ApplicationLogSignal::count)
                        .sum())
                .mapToInt(Integer::intValue)
                .sum();
    }

    static Optional<ApplicationLogSignal> firstLogSignal(AnalysisContext context, ApplicationLogSignalType type) {
        return context.applicationLogs().stream()
                .map(snapshot -> snapshot.signals().stream()
                        .filter(signal -> signal.type() == type)
                        .findFirst())
                .flatMap(Optional::stream)
                .findFirst();
    }

    static long countThreadsByNamePrefix(AnalysisContext context, List<String> prefixes) {
        return threadStream(context)
                .filter(thread -> prefixes.stream().anyMatch(prefix -> thread.name().startsWith(prefix)))
                .count();
    }

    static long countThreadsByStateAndPrefix(AnalysisContext context, List<String> prefixes, List<String> states) {
        return threadStream(context)
                .filter(thread -> prefixes.stream().anyMatch(prefix -> thread.name().startsWith(prefix)))
                .filter(thread -> states.contains(thread.state()))
                .count();
    }

    static long countThreadsWithStackKeyword(AnalysisContext context, List<String> keywords) {
        return threadStream(context)
                .filter(thread -> stackContainsAny(thread, keywords))
                .count();
    }

    static boolean hasDeadlock(AnalysisContext context) {
        return context.threadDumps().stream().anyMatch(ThreadDumpSnapshot::deadlockDetected);
    }

    static List<EvidenceRef> evidenceFromMetric(String metricName, double value) {
        return List.of(new EvidenceRef("actuator-metrics", metricName, metricName + "=" + value));
    }

    static List<EvidenceRef> evidenceFromThread(String threadName, String summary) {
        return List.of(new EvidenceRef("thread-dump", threadName, summary));
    }

    static List<EvidenceRef> evidenceFromLogSignal(ApplicationLogSignal signal) {
        return List.of(new EvidenceRef("application-log", signal.type().name(), signal.sampleLine()));
    }

    static List<String> defaultNextActions(String... actions) {
        return List.of(actions);
    }

    @SafeVarargs
    static List<EvidenceRef> combineEvidence(List<EvidenceRef>... groups) {
        List<EvidenceRef> all = new ArrayList<>();
        for (List<EvidenceRef> group : groups) {
            all.addAll(group);
        }
        return all.stream()
                .sorted(Comparator.comparing(EvidenceRef::artifactSource).thenComparing(EvidenceRef::locator))
                .toList();
    }

    private static Stream<ThreadDumpThread> threadStream(AnalysisContext context) {
        return context.threadDumps().stream()
                .map(ThreadDumpSnapshot::threads)
                .flatMap(List::stream);
    }

    private static boolean stackContainsAny(ThreadDumpThread thread, List<String> keywords) {
        return thread.stackLines().stream()
                .map(line -> line.toLowerCase(Locale.ROOT))
                .anyMatch(line -> keywords.stream().anyMatch(line::contains));
    }
}
