package io.github.jvmdoctor.cli;

import io.github.jvmdoctor.domain.Artifact;
import io.github.jvmdoctor.domain.ArtifactType;
import io.github.jvmdoctor.domain.IncidentInput;
import io.github.jvmdoctor.engine.IncidentAnalysisEngine;
import picocli.CommandLine;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "jvm-doctor",
        mixinStandardHelpOptions = true,
        description = "Analyze thread dumps, actuator metrics, and application logs into a triage report."
)
public class JvmDoctorCliCommand implements Callable<Integer> {

    @CommandLine.Option(
            names = "--thread-dump",
            description = "Path to a thread dump file. Can be repeated."
    )
    private List<Path> threadDumpPaths = new ArrayList<>();

    @CommandLine.Option(
            names = "--actuator-metrics",
            description = "Path to an actuator metrics snapshot. Can be repeated."
    )
    private List<Path> actuatorMetricsPaths = new ArrayList<>();

    @CommandLine.Option(
            names = "--log",
            description = "Path to an application log file. Can be repeated."
    )
    private List<Path> logPaths = new ArrayList<>();

    @CommandLine.Option(
            names = "--format",
            defaultValue = "markdown",
            description = "Output format. Supported values: ${COMPLETION-CANDIDATES}."
    )
    private ReportFormat format;

    @CommandLine.Option(
            names = "--output",
            description = "Optional output file path."
    )
    private Path output;

    private final IncidentAnalysisEngine engine = IncidentAnalysisEngine.bootstrap();
    private final AnalysisResultRenderer renderer = new AnalysisResultRenderer();

    @Override
    public Integer call() throws Exception {
        if (threadDumpPaths.isEmpty() && actuatorMetricsPaths.isEmpty() && logPaths.isEmpty()) {
            throw new CommandLine.ParameterException(
                    new CommandLine(this),
                    "At least one of --thread-dump, --actuator-metrics, or --log must be provided."
            );
        }

        IncidentInput input = new IncidentInput(buildArtifacts(), Map.of("mode", "cli"));
        String rendered = renderer.render(engine.analyze(input), format);

        if (output != null) {
            Path absoluteOutput = output.toAbsolutePath();
            Path parent = absoluteOutput.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(absoluteOutput, rendered, StandardCharsets.UTF_8);
        } else {
            System.out.println(rendered);
        }
        return CommandLine.ExitCode.OK;
    }

    private List<Artifact> buildArtifacts() {
        List<Artifact> artifacts = new ArrayList<>();
        for (Path threadDumpPath : threadDumpPaths) {
            artifacts.add(new Artifact(
                    ArtifactType.THREAD_DUMP,
                    threadDumpPath.getFileName().toString(),
                    threadDumpPath.toAbsolutePath()
            ));
        }
        for (Path actuatorMetricsPath : actuatorMetricsPaths) {
            artifacts.add(new Artifact(
                    ArtifactType.ACTUATOR_METRICS,
                    actuatorMetricsPath.getFileName().toString(),
                    actuatorMetricsPath.toAbsolutePath()
            ));
        }
        for (Path logPath : logPaths) {
            artifacts.add(new Artifact(
                    ArtifactType.APPLICATION_LOG,
                    logPath.getFileName().toString(),
                    logPath.toAbsolutePath()
            ));
        }
        return artifacts;
    }
}
