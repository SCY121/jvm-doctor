package io.github.jvmdoctor.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateAndFetchAnalysis() throws Exception {
        MockMultipartFile threadDump = new MockMultipartFile(
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
                "http-nio-8080-exec-3" #23 daemon
                   java.lang.Thread.State: WAITING (parking)
                        at java.net.SocketInputStream.socketRead0(Native Method)
                "http-nio-8080-exec-4" #24 daemon
                   java.lang.Thread.State: RUNNABLE
                        at java.net.SocketInputStream.socketRead0(Native Method)
                """.getBytes()
        );
        MockMultipartFile actuatorMetrics = new MockMultipartFile(
                "actuatorMetrics",
                "metrics.json",
                MediaType.APPLICATION_JSON_VALUE,
                """
                {
                  "names": ["hikaricp.connections.active", "hikaricp.connections.max", "tomcat.threads.current", "tomcat.threads.busy"],
                  "metrics": [
                    { "name": "hikaricp.connections.active", "measurements": [{ "statistic": "VALUE", "value": 10 }] },
                    { "name": "hikaricp.connections.max", "measurements": [{ "statistic": "VALUE", "value": 10 }] },
                    { "name": "tomcat.threads.current", "measurements": [{ "statistic": "VALUE", "value": 4 }] },
                    { "name": "tomcat.threads.busy", "measurements": [{ "statistic": "VALUE", "value": 4 }] }
                  ]
                }
                """.getBytes()
        );
        MockMultipartFile logFile = new MockMultipartFile(
                "logFile",
                "app.log",
                MediaType.TEXT_PLAIN_VALUE,
                """
                2026-04-17 21:00:01 ERROR HikariPool - Connection is not available, request timed out after 30000ms.
                2026-04-17 21:00:02 ERROR RemoteClient - java.net.SocketTimeoutException: Read timed out
                """.getBytes()
        );

        String body = mockMvc.perform(multipart("/api/v1/analyses")
                        .file(threadDump)
                        .file(actuatorMetrics)
                        .file(logFile))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.ai.status").value("DISABLED"))
                .andExpect(jsonPath("$.report.findings[0].ruleId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("DB_POOL_EXHAUSTED");

        String analysisId = body.replaceAll(".*\"analysisId\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/v1/analyses/{id}", analysisId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisId").value(analysisId));
    }

    @Test
    void shouldExposeRuleList() throws Exception {
        mockMvc.perform(get("/api/v1/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rules").isArray())
                .andExpect(jsonPath("$.rules[0]").exists());
    }
}
