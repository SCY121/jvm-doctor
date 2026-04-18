package io.github.jvmdoctor.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ActuatorSnapshotServiceTest {

    @Autowired
    private ActuatorSnapshotService actuatorSnapshotService;

    private HttpServer httpServer;

    @BeforeEach
    void setUp() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/actuator/metrics", exchange ->
                write(exchange, "application/json", """
                        {
                          "names": ["hikaricp.connections.active", "hikaricp.connections.max", "tomcat.threads.current", "tomcat.threads.busy"]
                        }
                        """));
        httpServer.createContext("/actuator/metrics/hikaricp.connections.active", exchange ->
                write(exchange, "application/json", """
                        { "name": "hikaricp.connections.active", "measurements": [{ "statistic": "VALUE", "value": 10 }] }
                        """));
        httpServer.createContext("/actuator/metrics/hikaricp.connections.max", exchange ->
                write(exchange, "application/json", """
                        { "name": "hikaricp.connections.max", "measurements": [{ "statistic": "VALUE", "value": 10 }] }
                        """));
        httpServer.createContext("/actuator/metrics/tomcat.threads.current", exchange ->
                write(exchange, "application/json", """
                        { "name": "tomcat.threads.current", "measurements": [{ "statistic": "VALUE", "value": 4 }] }
                        """));
        httpServer.createContext("/actuator/metrics/tomcat.threads.busy", exchange ->
                write(exchange, "application/json", """
                        { "name": "tomcat.threads.busy", "measurements": [{ "statistic": "VALUE", "value": 4 }] }
                        """));
        httpServer.createContext("/actuator/threaddump", exchange ->
                write(exchange, "text/plain", """
                        "http-nio-8080-exec-1" #21 daemon
                           java.lang.Thread.State: RUNNABLE
                                at java.net.SocketInputStream.socketRead0(Native Method)
                        "http-nio-8080-exec-2" #22 daemon
                           java.lang.Thread.State: WAITING (parking)
                                at java.net.SocketInputStream.socketRead0(Native Method)
                        "http-nio-8080-exec-3" #23 daemon
                           java.lang.Thread.State: WAITING (parking)
                                at java.net.SocketInputStream.socketRead0(Native Method)
                        "http-nio-8080-exec-4" #24 daemon
                           java.lang.Thread.State: RUNNABLE
                                at java.net.SocketInputStream.socketRead0(Native Method)
                        """));
        httpServer.start();
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    void shouldAnalyzeActuatorSnapshot() {
        String baseUrl = "http://localhost:" + httpServer.getAddress().getPort();
        AnalysisResponse response = actuatorSnapshotService.analyzeSnapshot(new ActuatorSnapshotRequest(
                baseUrl,
                List.of("hikaricp.connections.active", "hikaricp.connections.max", "tomcat.threads.current", "tomcat.threads.busy"),
                null,
                null
        ));

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.report().findings())
                .extracting(finding -> finding.ruleId())
                .contains("DB_POOL_EXHAUSTED", "HTTP_THREAD_POOL_EXHAUSTED");
    }

    private void write(HttpExchange exchange, String contentType, String payload) throws IOException {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType + "; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
