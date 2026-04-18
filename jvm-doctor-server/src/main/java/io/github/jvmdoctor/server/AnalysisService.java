package io.github.jvmdoctor.server;

import io.github.jvmdoctor.domain.AnalysisResult;
import io.github.jvmdoctor.domain.Artifact;
import io.github.jvmdoctor.domain.ArtifactType;
import io.github.jvmdoctor.domain.IncidentInput;
import io.github.jvmdoctor.engine.IncidentAnalysisEngine;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AnalysisService {

    private final IncidentAnalysisEngine incidentAnalysisEngine;
    private final AiAugmentationService aiAugmentationService;
    private final ArtifactStorage artifactStorage;
    private final Map<String, StoredAnalysis> analyses = new ConcurrentHashMap<>();

    public AnalysisService(
            IncidentAnalysisEngine incidentAnalysisEngine,
            AiAugmentationService aiAugmentationService,
            ArtifactStorage artifactStorage
    ) {
        this.incidentAnalysisEngine = incidentAnalysisEngine;
        this.aiAugmentationService = aiAugmentationService;
        this.artifactStorage = artifactStorage;
    }

    public AnalysisResponse analyzeUploads(
            List<MultipartFile> threadDumps,
            List<MultipartFile> actuatorMetrics,
            List<MultipartFile> logs
    ) {
        List<Artifact> artifacts = new ArrayList<>();
        artifacts.addAll(storeMultipartFiles(threadDumps, ArtifactType.THREAD_DUMP));
        artifacts.addAll(storeMultipartFiles(actuatorMetrics, ArtifactType.ACTUATOR_METRICS));
        artifacts.addAll(storeMultipartFiles(logs, ArtifactType.APPLICATION_LOG));
        return analyzeArtifacts(artifacts, Map.of("source", "upload"));
    }

    public AnalysisResponse analyzeArtifacts(List<Artifact> artifacts, Map<String, String> metadata) {
        if (artifacts.isEmpty()) {
            throw new IllegalArgumentException("At least one artifact is required");
        }

        AnalysisResult result = incidentAnalysisEngine.analyze(new IncidentInput(artifacts, metadata));
        String analysisId = UUID.randomUUID().toString();
        AiAugmentation aiAugmentation = aiAugmentationService.augment(result);
        AnalysisResponse response = toResponse(analysisId, result, aiAugmentation);
        analyses.put(analysisId, new StoredAnalysis(analysisId, Instant.now(), response));
        return response;
    }

    public AnalysisResponse getAnalysis(String analysisId) {
        StoredAnalysis storedAnalysis = analyses.get(analysisId);
        if (storedAnalysis == null) {
            throw new AnalysisNotFoundException(analysisId);
        }
        return storedAnalysis.response();
    }

    public List<String> listRuleIds() {
        return io.github.jvmdoctor.rules.RuleRegistry.defaults().rules().stream()
                .map(rule -> rule.id())
                .toList();
    }

    public ArtifactStorage artifactStorage() {
        return artifactStorage;
    }

    private List<Artifact> storeMultipartFiles(List<MultipartFile> files, ArtifactType type) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        return files.stream()
                .filter(file -> !file.isEmpty())
                .map(file -> artifactStorage.storeUpload(file, type))
                .toList();
    }

    private AnalysisResponse toResponse(String analysisId, AnalysisResult result, AiAugmentation aiAugmentation) {
        return new AnalysisResponse(analysisId, "COMPLETED", result.overview(), result.report(), aiAugmentation);
    }
}
