package com.example.cron_scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsJobRunsItAndReturnsExecutionHistory() throws Exception {
        String request = """
                {"name":"integration-test-job","cronExpression":"0 0 0 1 1 ?","maxRetries":2}
                """;

        mockMvc.perform(post("/api/jobs")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("integration-test-job"))
                .andExpect(jsonPath("$.enabled").value(true));

        mockMvc.perform(post("/api/jobs/1/run").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(get("/api/jobs/1/executions").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobName").value("integration-test-job"));
    }

    @Test
    void rejectsInvalidCronExpression() throws Exception {
        mockMvc.perform(post("/api/jobs")
                                                .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"bad-job\",\"cronExpression\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsDuplicateJobNames() throws Exception {
        String request = "{\"name\":\"unique-job\",\"cronExpression\":\"0 0 0 1 1 ?\"}";
        String duplicateRequest = "{\"name\":\"UNIQUE-JOB\",\"cronExpression\":\"0 0 0 1 1 ?\"}";

        mockMvc.perform(post("/api/jobs")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/jobs")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateRequest))
                .andExpect(status().isConflict());
    }
}