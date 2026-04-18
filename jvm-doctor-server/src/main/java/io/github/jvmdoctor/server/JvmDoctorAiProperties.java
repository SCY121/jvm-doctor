package io.github.jvmdoctor.server;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "jvm-doctor.ai")
public class JvmDoctorAiProperties {

    private boolean enabled;
    private String baseUrl = "";
    private String apiKey = "";
    private String model = "";
    private Duration timeout = Duration.ofSeconds(20);
    private double temperature = 0.1d;
    private int maxFindings = 8;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model == null ? "" : model;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout == null ? Duration.ofSeconds(20) : timeout;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxFindings() {
        return maxFindings;
    }

    public void setMaxFindings(int maxFindings) {
        this.maxFindings = maxFindings;
    }
}
