package io.github.jvmdoctor.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvmdoctor.domain.AnalysisResult;
import io.github.jvmdoctor.domain.Finding;
import io.github.jvmdoctor.domain.Hypothesis;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OpenAiCompatibleAugmentationService implements AiAugmentationService {

    private static final String PROVIDER = "openai-compatible";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final JvmDoctorAiProperties properties;

    public OpenAiCompatibleAugmentationService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            JvmDoctorAiProperties properties
    ) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(stripTrailingSlash(properties.getBaseUrl()))
                .requestFactory(createRequestFactory(properties.getTimeout()))
                .build();
    }

    @Override
    public AiAugmentation augment(AnalysisResult analysisResult) {
        if (!properties.isEnabled()) {
            return AiAugmentation.disabled("AI augmentation is disabled.");
        }
        if (isBlank(properties.getBaseUrl()) || isBlank(properties.getApiKey()) || isBlank(properties.getModel())) {
            return AiAugmentation.failed(PROVIDER, blankToNull(properties.getModel()), "AI is enabled but base URL, API key, or model is missing.");
        }

        try {
            String responseBody = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(properties.getApiKey()))
                    .body(buildRequestPayload(analysisResult))
                    .retrieve()
                    .body(String.class);

            AiStructuredResponse structuredResponse = parseStructuredResponse(responseBody);
            return AiAugmentation.completed(
                    PROVIDER,
                    properties.getModel(),
                    structuredResponse.summary(),
                    structuredResponse.recommendedActions(),
                    structuredResponse.missingEvidence(),
                    structuredResponse.riskNote()
            );
        } catch (Exception exception) {
            return AiAugmentation.failed(PROVIDER, blankToNull(properties.getModel()), rootMessage(exception));
        }
    }

    private Map<String, Object> buildRequestPayload(AnalysisResult analysisResult) {
        return Map.of(
                "model", properties.getModel(),
                "temperature", properties.getTemperature(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt()),
                        Map.of("role", "user", "content", userPrompt(analysisResult))
                )
        );
    }

    private String systemPrompt() {
        return """
                You are a JVM incident triage assistant.
                Use only the supplied deterministic evidence.
                Do not invent unsupported causes.
                Return only a JSON object with:
                summary: string
                recommendedActions: string[]
                missingEvidence: string[]
                riskNote: string
                """;
    }

    private String userPrompt(AnalysisResult analysisResult) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("overview", Map.of(
                "artifactCount", analysisResult.overview().artifactCount(),
                "threadDumpCount", analysisResult.overview().threadDumpCount(),
                "totalThreadCount", analysisResult.overview().totalThreadCount(),
                "threadStateCounts", analysisResult.overview().threadStateCounts(),
                "actuatorMetricsFileCount", analysisResult.overview().actuatorMetricsFileCount(),
                "applicationLogFileCount", analysisResult.overview().applicationLogFileCount(),
                "detectedLogSignalCount", analysisResult.overview().detectedLogSignalCount()
        ));
        payload.put("findings", analysisResult.report().findings().stream()
                .limit(Math.max(1, properties.getMaxFindings()))
                .map(this::toFindingPayload)
                .toList());
        payload.put("topHypotheses", analysisResult.report().topHypotheses().stream()
                .limit(3)
                .map(this::toHypothesisPayload)
                .toList());
        payload.put("recommendedActions", analysisResult.report().recommendedActions());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize AI prompt payload", exception);
        }
    }

    private Map<String, Object> toFindingPayload(Finding finding) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ruleId", finding.ruleId());
        payload.put("severity", finding.severity().name());
        payload.put("confidence", finding.confidence());
        payload.put("summary", finding.summary());
        payload.put("evidence", finding.evidenceRefs().stream()
                .map(ref -> Map.of(
                        "artifactSource", ref.artifactSource(),
                        "locator", ref.locator(),
                        "summary", ref.summary()
                ))
                .toList());
        payload.put("nextActions", finding.nextActions());
        return payload;
    }

    private Map<String, Object> toHypothesisPayload(Hypothesis hypothesis) {
        return Map.of(
                "title", hypothesis.title(),
                "score", hypothesis.score(),
                "explanation", hypothesis.explanation(),
                "supportingFindingIds", hypothesis.supportingFindingIds(),
                "missingData", hypothesis.missingData()
        );
    }

    private AiStructuredResponse parseStructuredResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
                throw new IllegalStateException("AI provider returned an empty message content.");
            }

            String jsonPayload = extractJsonPayload(contentNode.asText());
            JsonNode structured = objectMapper.readTree(jsonPayload);

            String summary = structured.path("summary").asText("").trim();
            if (summary.isBlank()) {
                throw new IllegalStateException("AI response did not include a summary.");
            }

            return new AiStructuredResponse(
                    summary,
                    readStringList(structured.path("recommendedActions")),
                    readStringList(structured.path("missingEvidence")),
                    blankToNull(structured.path("riskNote").asText(""))
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse AI response: " + rootMessage(exception), exception);
        }
    }

    private List<String> readStringList(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            String value = item.asText("").trim();
            if (!value.isBlank()) {
                values.add(value);
            }
        });
        return values;
    }

    private String extractJsonPayload(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace >= firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }
        return trimmed;
    }

    private SimpleClientHttpRequestFactory createRequestFactory(Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return requestFactory;
    }

    private String stripTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    private record AiStructuredResponse(
            String summary,
            List<String> recommendedActions,
            List<String> missingEvidence,
            String riskNote
    ) {
    }
}
