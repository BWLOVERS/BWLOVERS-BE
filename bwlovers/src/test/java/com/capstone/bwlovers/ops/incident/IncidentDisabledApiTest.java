package com.capstone.bwlovers.ops.incident;

import com.capstone.bwlovers.BwloversApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = BwloversApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class IncidentDisabledApiTest {
    @Autowired MockMvc mvc;

    @Test void disabledFeatureDoesNotExposeWebhookOrMetricsAndExistingApisStillRequireJwt() throws Exception {
        mvc.perform(post("/internal/ai-incidents").header("X-Alert-Token", "a".repeat(32))
                .contentType("application/json").content("{}")).andExpect(status().isNotFound());
        mvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer anything")).andExpect(status().isNotFound());
        mvc.perform(get("/health/status")).andExpect(status().isUnauthorized());
    }
}
