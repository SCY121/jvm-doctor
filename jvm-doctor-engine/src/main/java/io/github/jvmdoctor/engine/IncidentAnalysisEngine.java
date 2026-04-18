package io.github.jvmdoctor.engine;

import io.github.jvmdoctor.ai.HypothesisGenerator;
import io.github.jvmdoctor.ai.RuleBasedHypothesisGenerator;
import io.github.jvmdoctor.domain.ActuatorMetricsSnapshot;
import io.github.jvmdoctor.domain.AnalysisContext;
import io.github.jvmdoctor.domain.AnalysisOverview;
import io.github.jvmdoctor.domain.AnalysisResult;
import io.github.jvmdoctor.domain.ApplicationLogSnapshot;
import io.github.jvmdoctor.domain.Artifact;
import io.github.jvmdoctor.domain.ArtifactType;
import io.github.jvmdoctor.domain.Finding;
import io.github.jvmdoctor.domain.IncidentInput;
import io.github.jvmdoctor.domain.IncidentReport;
import io.github.jvmdoctor.domain.ThreadDumpSnapshot;
import io.github.jvmdoctor.parser.ActuatorMetricsParser;
import io.github.jvmdoctor.parser.ApplicationLogParser;
import io.github.jvmdoctor.parser.ThreadDumpParser;
import io.github.jvmdoctor.rules.FindingRule;
import io.github.jvmdoctor.rules.RuleRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public final class IncidentAnalysisEngine {

    private final List<FindingRule> rules;
    private final HypothesisGenerator hypothesisGenerator;
    private final ThreadDumpParser threadDumpParser;
    private final ActuatorMetricsParser actuatorMetricsParser;
    private final ApplicationLogParser applicationLogParser;

    public IncidentAnalysisEngine(
            List<FindingRule> rules,
            HypothesisGenerator hypothesisGenerator,
            ThreadDumpParser threadDumpParser,
            ActuatorMetricsParser actuatorMetricsParser,
            ApplicationLogParser applicationLogParser
    ) {
        this.rules = List.copyOf(Objects.requireNonNull(rules, "rules must not be null"));
        this.hypothesisGenerator = Objects.requireNonNull(hypothesisGenerator, "hypothesisGenerator must not be null");
        this.threadDumpParser = Objects.requireNonNull(threadDumpParser, "threadDumpParser must not be null");
        this.actuatorMetricsParser = Objects.requireNonNull(actuatorMetricsParser, "actuatorMetricsParser must not be null");
        this.applicationLogParser = Objects.requireNonNull(applicationLogParser, "applicationLogParser must not be null");
    }

    public static IncidentAnalysisEngine bootstrap() {
        return new IncidentAnalysisEngine(
                RuleRegistry.defaults().rules(),
                new RuleBasedHypothesisGenerator(),
                new ThreadDumpParser(),
                new ActuatorMetricsParser(),
                new ApplicationLogParser()
        );
    }

    public AnalysisResult analyze(IncidentInput input) {
        AnalysisContext context = buildContext(input);
        List<Finding> findings = new ArrayList<>();
        for (FindingRule rule : rules) {
            findings.addAll(rule.evaluate(context));
        }
        findings = findings.stream()
                .sorted(Comparator
                        .comparing((Finding finding) -> finding.severity().ordinal()).reversed()
                        .thenComparing(Finding::confidence, Comparator.reverseOrder()))
                .toList();

        AnalysisOverview overview = buildOverview(context);
        var hypotheses = hypothesisGenerator.generate(context, findings);
        var report = new IncidentReport(
                findings,
                hypotheses,
                buildExecutiveSummary(overview),
                buildRecommendedActions(context)
        );

        return new AnalysisResult(context, overview, report);
    }

    private AnalysisContext buildContext(IncidentInput input) {
        List<ThreadDumpSnapshot> threadDumps = new ArrayList<>();
        List<ActuatorMetricsSnapshot> actuatorMetrics = new ArrayList<>();
        List<ApplicationLogSnapshot> applicationLogs = new ArrayList<>();

        for (Artifact artifact : input.artifacts()) {
            if (artifact.type() == ArtifactType.THREAD_DUMP || artifact.type() == ArtifactType.ACTUATOR_THREAD_DUMP) {
                threadDumps.add(threadDumpParser.parse(artifact));
            }
            if (artifact.type() == ArtifactType.ACTUATOR_METRICS) {
                actuatorMetrics.add(actuatorMetricsParser.parse(artifact));
            }
            if (artifact.type() == ArtifactType.APPLICATION_LOG) {
                applicationLogs.add(applicationLogParser.parse(artifact));
            }
        }

        return new AnalysisContext(input, threadDumps, actuatorMetrics, applicationLogs);
    }

    private AnalysisOverview buildOverview(AnalysisContext context) {
        Map<String, Long> threadStateCounts = new TreeMap<>();
        int totalThreadCount = 0;
        for (ThreadDumpSnapshot snapshot : context.threadDumps()) {
            totalThreadCount += snapshot.threadCount();
            snapshot.stateCounts().forEach((state, count) -> threadStateCounts.merge(state, count, Long::sum));
        }

        int availableMetricNameCount = context.actuatorMetrics().stream()
                .mapToInt(ActuatorMetricsSnapshot::metricNameCount)
                .sum();
        int detailedMetricCount = context.actuatorMetrics().stream()
                .mapToInt(ActuatorMetricsSnapshot::detailedMetricCount)
                .sum();
        int detectedLogSignalCount = context.applicationLogs().stream()
                .mapToInt(snapshot -> snapshot.signals().size())
                .sum();

        return new AnalysisOverview(
                context.input().artifacts().size(),
                context.threadDumps().size(),
                totalThreadCount,
                threadStateCounts,
                context.actuatorMetrics().size(),
                availableMetricNameCount,
                detailedMetricCount,
                context.applicationLogs().size(),
                detectedLogSignalCount
        );
    }

    private String buildExecutiveSummary(AnalysisOverview overview) {
        return "Parsed "
                + overview.threadDumpCount()
                + " thread dump file(s), "
                + overview.actuatorMetricsFileCount()
                + " actuator metrics snapshot(s), and "
                + overview.applicationLogFileCount()
                + " application log file(s); detected "
                + overview.detectedLogSignalCount()
                + " log signal(s).";
    }

    private List<String> buildRecommendedActions(AnalysisContext context) {
        List<String> actions = new ArrayList<>();
        if (context.threadDumps().isEmpty()) {
            actions.add("Add at least one thread dump.");
        }
        if (context.actuatorMetrics().isEmpty()) {
            actions.add("Add at least one actuator metrics snapshot.");
        }
        if (context.applicationLogs().isEmpty()) {
            actions.add("Add at least one application log file.");
        }
        actions.add("Use the matched findings to inspect thread pools, connection pools, and downstream timeouts first.");
        actions.add("If the root cause is still unclear, capture JFR or async-profiler data next.");
        return actions;
    }
}
