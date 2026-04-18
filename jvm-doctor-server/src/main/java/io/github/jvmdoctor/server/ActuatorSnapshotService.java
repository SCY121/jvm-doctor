package io.github.jvmdoctor.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvmdoctor.domain.Artifact;
import io.github.jvmdoctor.domain.ArtifactType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ActuatorSnapshotService {

    private static final List<String> DEFAULT_METRICS = List.of(
            "jvm.threads.live",
            "hikaricp.connections.active",
            "hikaricp.connections.max",
            "tomcat.threads.current",
            "tomcat.threads.busy",
            "system.cpu.usage",
            "process.cpu.usage",
            "jvm.gc.pause"
    );

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final AnalysisService analysisService;

    public ActuatorSnapshotService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, AnalysisService analysisService) {
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
        this.analysisService = analysisService;
    }

    public AnalysisResponse analyzeSnapshot(ActuatorSnapshotRequest request) {
        try {
            RestClient restClient = restClientBuilder
                    .baseUrl(stripTrailingSlash(request.baseUrl()))
                    .defaultHeaders(headers -> applyAuth(headers, request))
                    .build();

            JsonNode metricsRoot = objectMapper.readTree(fetchJson(restClient, "/actuator/metrics"));
            List<String> availableNames = new ArrayList<>();
            if (metricsRoot.has("names")) {
                metricsRoot.get("names").forEach(node -> availableNames.add(node.asText()));
            }

            List<String> requestedMetrics = request.metricNames() == null || request.metricNames().isEmpty()
                    ? DEFAULT_METRICS
                    : request.metricNames();

            List<JsonNode> detailedMetrics = new ArrayList<>();
            for (String metricName : requestedMetrics) {
                if (availableNames.contains(metricName)) {
                    detailedMetrics.add(objectMapper.readTree(fetchJson(restClient, "/actuator/metrics/" + metricName)));
                }
            }

            Map<String, Object> mergedMetrics = new LinkedHashMap<>();
            mergedMetrics.put("names", availableNames);
            mergedMetrics.put("metrics", detailedMetrics);

            String threadDump = fetchText(restClient, "/actuator/threaddump");

            Artifact metricsArtifact = analysisService.artifactStorage()
                    .storeText("actuator-metrics", ArtifactType.ACTUATOR_METRICS, ".json", objectMapper.writeValueAsString(mergedMetrics));
            Artifact threadDumpArtifact = analysisService.artifactStorage()
                    .storeText("actuator-threaddump", ArtifactType.THREAD_DUMP, ".txt", threadDump);

            return analysisService.analyzeArtifacts(
                    List.of(threadDumpArtifact, metricsArtifact),
                    Map.of("source", "actuator-snapshot", "baseUrl", request.baseUrl())
            );
        } catch (RestClientException exception) {
            throw new SnapshotFetchException("Failed to fetch actuator snapshot", exception);
        } catch (IOException exception) {
            throw new SnapshotFetchException("Failed to process actuator snapshot payload", exception);
        }
    }

    private String fetchJson(RestClient restClient, String path) {
        return restClient.get()
                .uri(path)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);
    }

    private String fetchText(RestClient restClient, String path) {
        return restClient.get()
                .uri(path)
                .accept(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);
    }

    private void applyAuth(HttpHeaders headers, ActuatorSnapshotRequest request) {
        if (request.username() != null && !request.username().isBlank()) {
            String token = Base64.getEncoder().encodeToString((request.username() + ":" + (request.password() == null ? "" : request.password())).getBytes());
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + token);
        }
    }

    private String stripTrailingSlash(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
