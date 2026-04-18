package io.github.jvmdoctor.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalysisControllerAiFallbackTest {

    private static HttpServer httpServer;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("jvm-doctor.ai.enabled", () -> true);
        registry.add("jvm-doctor.ai.base-url", AnalysisControllerAiFallbackTest::baseUrl);
        registry.add("jvm-doctor.ai.api-key", () -> "test-key");
        registry.add("jvm-doctor.ai.model", () -> "test-model");
    }

    @AfterAll
    static void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
    }

    @Test
    void shouldFallBackToDeterministicReportWhenAiFails() throws Exception {
        mockMvc.perform(multipart("/api/v1/analyses")
                        .file(threadDump())
                        .file(actuatorMetrics())
                        .file(logFile()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.report.findings[0].ruleId").exists())
                .andExpect(jsonPath("$.ai.status").value("FAILED"))
                .andExpect(jsonPath("$.ai.failureReason").exists());
    }

    private static synchronized String baseUrl() {
        if (httpServer == null) {
            try {
                httpServer = HttpServer.create(new InetSocketAddress(0), 0);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to create mock AI server", exception);
            }
            httpServer.createContext("/chat/completions", exchange ->
                    write(exchange, 500, "{\"error\":{\"message\":\"provider unavailable\"}}"));
            httpServer.start();
        }
        return "http://localhost:" + httpServer.getAddress().getPort();
    }

    private static MockMultipartFile threadDump() {
        return new MockMultipartFile(
                "threadDump",
                "thread-dump.txt",
                MediaType.TEXT_PLAIN_VALUE,
                """
                "http-nio-8080-exec-1" #21 daemon
                   java.lang.Thread.State: RUNNABLE
                        at java.net.SocketInputStream.socketRead0(Native Method)
                "http-nio-8080-exec-2" #22 daemon
                   java.lang.Thread.State: WAITING (parking)
                        at java.net.SocketInputStream.socketRead0(Native Method)
                """.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static MockMultipartFile actuatorMetrics() {
        return new MockMultipartFile(
                "actuatorMetrics",
                "metrics.json",
                MediaType.APPLICATION_JSON_VALUE,
                """
                {
                  "names": ["hikaricp.connections.active", "hikaricp.connections.max"],
                  "metrics": [
                    { "name": "hikaricp.connections.active", "measurements": [{ "statistic": "VALUE", "value": 10 }] },
                    { "name": "hikaricp.connections.max", "measurements": [{ "statistic": "VALUE", "value": 10 }] }
                  ]
                }
                """.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static MockMultipartFile logFile() {
        return new MockMultipartFile(
                "logFile",
                "app.log",
                MediaType.TEXT_PLAIN_VALUE,
                """
                2026-04-17 21:00:01 ERROR HikariPool - Connection is not available, request timed out after 30000ms.
                """.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static void write(HttpExchange exchange, int status, String payload) throws IOException {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
