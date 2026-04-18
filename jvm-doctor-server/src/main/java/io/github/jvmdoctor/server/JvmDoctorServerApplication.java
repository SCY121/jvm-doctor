package io.github.jvmdoctor.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvmdoctor.engine.IncidentAnalysisEngine;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication
@EnableConfigurationProperties(JvmDoctorAiProperties.class)
public class JvmDoctorServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(JvmDoctorServerApplication.class, args);
    }

    @Bean
    IncidentAnalysisEngine incidentAnalysisEngine() {
        return IncidentAnalysisEngine.bootstrap();
    }

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    AiAugmentationService aiAugmentationService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            JvmDoctorAiProperties properties
    ) {
        return new OpenAiCompatibleAugmentationService(restClientBuilder, objectMapper, properties);
    }
}
