package io.github.jvmdoctor.server;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AboutController {

    @GetMapping("/about")
    public Map<String, Object> about() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", "jvm-doctor");
        payload.put("phase", "v0");
        payload.put("capabilities", new String[]{
                "thread-dump-analysis",
                "actuator-metrics-analysis",
                "application-log-analysis",
                "rule-based-findings",
                "actuator-snapshot",
                "optional-ai-augmentation"
        });
        return payload;
    }
}
