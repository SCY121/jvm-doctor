package io.github.jvmdoctor.server;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiAugmentation(
        AiAugmentationStatus status,
        String provider,
        String model,
        String summary,
        List<String> recommendedActions,
        List<String> missingEvidence,
        String riskNote,
        String failureReason
) {

    public AiAugmentation {
        Objects.requireNonNull(status, "status must not be null");
        recommendedActions = List.copyOf(recommendedActions == null ? List.of() : recommendedActions);
        missingEvidence = List.copyOf(missingEvidence == null ? List.of() : missingEvidence);
    }

    public static AiAugmentation disabled(String reason) {
        return new AiAugmentation(
                AiAugmentationStatus.DISABLED,
                null,
                null,
                null,
                List.of(),
                List.of(),
                null,
                reason
        );
    }

    public static AiAugmentation completed(
            String provider,
            String model,
            String summary,
            List<String> recommendedActions,
            List<String> missingEvidence,
            String riskNote
    ) {
        return new AiAugmentation(
                AiAugmentationStatus.COMPLETED,
                provider,
                model,
                summary,
                recommendedActions,
                missingEvidence,
                riskNote,
                null
        );
    }

    public static AiAugmentation failed(String provider, String model, String failureReason) {
        return new AiAugmentation(
                AiAugmentationStatus.FAILED,
                provider,
                model,
                null,
                List.of(),
                List.of(),
                null,
                failureReason
        );
    }
}
