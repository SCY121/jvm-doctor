package io.github.jvmdoctor.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AboutController.class)
class AboutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnAboutPayload() throws Exception {
        mockMvc.perform(get("/api/v1/about"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("jvm-doctor"))
                .andExpect(jsonPath("$.phase").value("v0"))
                .andExpect(jsonPath("$.capabilities[5]").value("optional-ai-augmentation"));
    }
}
