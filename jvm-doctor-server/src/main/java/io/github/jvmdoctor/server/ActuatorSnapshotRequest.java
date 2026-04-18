package io.github.jvmdoctor.server;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ActuatorSnapshotRequest(
        @NotBlank String baseUrl,
        List<String> metricNames,
        String username,
        String password
) {
}
