package com.example.cron_scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CronSchedulerApplicationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void contextLoads() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/api/jobs/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalJobs").exists())
            .andExpect(jsonPath("$.activeJobs").exists())
            .andExpect(jsonPath("$.pausedJobs").exists())
            .andExpect(jsonPath("$.successRate").isNumber());
    }
}
