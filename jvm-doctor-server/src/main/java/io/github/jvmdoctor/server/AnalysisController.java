package io.github.jvmdoctor.server;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final ActuatorSnapshotService actuatorSnapshotService;

    public AnalysisController(AnalysisService analysisService, ActuatorSnapshotService actuatorSnapshotService) {
        this.analysisService = analysisService;
        this.actuatorSnapshotService = actuatorSnapshotService;
    }

    @PostMapping(path = "/analyses", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisResponse createAnalysis(
            @RequestPart(name = "threadDump", required = false) List<MultipartFile> threadDumps,
            @RequestPart(name = "actuatorMetrics", required = false) List<MultipartFile> actuatorMetrics,
            @RequestPart(name = "logFile", required = false) List<MultipartFile> logs
    ) {
        return analysisService.analyzeUploads(threadDumps, actuatorMetrics, logs);
    }

    @GetMapping("/analyses/{id}")
    public AnalysisResponse getAnalysis(@PathVariable("id") String id) {
        return analysisService.getAnalysis(id);
    }

    @GetMapping("/rules")
    public Map<String, List<String>> rules() {
        return Map.of("rules", analysisService.listRuleIds());
    }

    @PostMapping(path = "/actuator/snapshot", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AnalysisResponse snapshot(@Valid @RequestBody ActuatorSnapshotRequest request) {
        return actuatorSnapshotService.analyzeSnapshot(request);
    }
}
