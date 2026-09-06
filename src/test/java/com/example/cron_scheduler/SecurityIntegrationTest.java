package com.example.cron_scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void operatorCanReadJobsApi() throws Exception {
        mockMvc.perform(get("/api/jobs").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanAccessJobsApi() throws Exception {
        mockMvc.perform(get("/api/jobs")
                        .with(user("admin").roles("ADMIN"))
                        .header("X-Request-ID", "demo-request-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", "demo-request-123"));
    }

    @Test
    void operatorCannotDeleteJobs() throws Exception {
        mockMvc.perform(delete("/api/jobs/1").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isForbidden());
    }
}
