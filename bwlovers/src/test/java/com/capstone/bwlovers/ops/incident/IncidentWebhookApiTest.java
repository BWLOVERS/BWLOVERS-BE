package com.capstone.bwlovers.ops.incident;

import com.capstone.bwlovers.BwloversApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static com.capstone.bwlovers.ops.incident.IncidentJson.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = BwloversApplication.class, properties = {
        "incident.webhook.enabled=true", "incident.webhook.token=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "incident.webhook.metrics-token=mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm", "incident.webhook.api-key=test-key",
        "incident.webhook.discord-webhook-url=https://discord.com/api/webhooks/123/test-token",
        "incident.webhook.prometheus-url=http://127.0.0.1:9090", "incident.webhook.worker-enabled=false"
})
@ActiveProfiles({"test", "incident"})
@AutoConfigureMockMvc
@AutoConfigureObservability
class IncidentWebhookApiTest {
    private static final String TOKEN = "a".repeat(32);
    @Autowired MockMvc mvc;
    @MockitoBean IncidentJobStore store;

    @BeforeEach void setup() { when(store.enqueue(anyList(), any())).thenReturn(MAPPER.createArrayNode()); }

    @Test void webhookUsesDedicatedTokenAndRejectsApplicationJwtOrMissingToken() throws Exception {
        String body = PrometheusIncidentCollectorTest.webhook().toString();
        mvc.perform(post("/internal/ai-incidents").contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/ai-incidents").header("Authorization", "Bearer application-jwt").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/ai-incidents").header("X-Alert-Token", TOKEN).contentType("application/json").content(body))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.jobs").isArray());
        verify(store, times(1)).enqueue(anyList(), any());
        mvc.perform(post("/internal/ai-incidents").header("Authorization", "Bearer " + TOKEN).contentType("application/json").content(body))
                .andExpect(status().isAccepted());
        verify(store, times(2)).enqueue(anyList(), any());
    }

    @Test void rejectsOversizedTruncatedOrInvalidBodiesWithoutEnqueue() throws Exception {
        mvc.perform(post("/internal/ai-incidents").header("X-Alert-Token", TOKEN).contentType("application/json").content("x".repeat(65537)))
                .andExpect(status().isPayloadTooLarge());
        mvc.perform(post("/internal/ai-incidents").header("X-Alert-Token", TOKEN).contentType("application/json")
                .content(PrometheusIncidentCollectorTest.webhook().put("truncatedAlerts", 1).toString())).andExpect(status().isBadRequest());
        mvc.perform(post("/internal/ai-incidents").header("X-Alert-Token", TOKEN).contentType("application/json").content("{\"secret\":broken"))
                .andExpect(status().isBadRequest()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("broken"))));
        verify(store, never()).enqueue(anyList(), any());
    }

    @Test void queueCapacityAndRedisFailureNeverReturnAccepted() throws Exception {
        when(store.enqueue(anyList(), any())).thenThrow(new IncidentQueueFullException());
        mvc.perform(post("/internal/ai-incidents").header("X-Alert-Token", TOKEN).contentType("application/json")
                .content(PrometheusIncidentCollectorTest.webhook().toString())).andExpect(status().isTooManyRequests());
        doThrow(new RedisConnectionFailureException("secret connection details")).when(store).enqueue(anyList(), any());
        mvc.perform(post("/internal/ai-incidents").header("X-Alert-Token", TOKEN).contentType("application/json")
                .content(PrometheusIncidentCollectorTest.webhook().toString())).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("INCIDENT_STORE_UNAVAILABLE"));
    }

    @Test void statusAndManualRetryRequireTokenAndDeliveryCheck() throws Exception {
        String id = "b".repeat(64);
        when(store.find(id)).thenReturn(Optional.of(object().put("jobId", id).put("owner", "private-owner")
                .put("status", "FAILED").put("reportJson", "{\"analyzedInput\":{\"relatedLogs\":[]}}")));
        mvc.perform(get("/internal/ai-incidents/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(get("/internal/ai-incidents/" + id).header("X-Alert-Token", TOKEN)).andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").doesNotExist()).andExpect(jsonPath("$.reportJson").doesNotExist())
                .andExpect(jsonPath("$.report.analyzedInput.relatedLogs").isArray());
        mvc.perform(post("/internal/ai-incidents/" + id + "/retry").header("X-Alert-Token", TOKEN)
                .contentType("application/json").content("{\"deliveryChecked\":false}")).andExpect(status().isBadRequest());
        verify(store, never()).retry(anyString(), any());
        when(store.retry(eq(id), any())).thenReturn(true);
        mvc.perform(post("/internal/ai-incidents/" + id + "/retry").header("X-Alert-Token", TOKEN)
                .contentType("application/json").content("{\"deliveryChecked\":true}")).andExpect(status().isAccepted());
    }

    @Test void prometheusRequiresSeparateCredentialAndExposesActualJvmAndPoolMetrics() throws Exception {
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/prometheus").header("X-Alert-Token", TOKEN)).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer " + "m".repeat(32))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jvm_memory_used_bytes")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("hikaricp_connections_active")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("http_server_requests_seconds_bucket")));
    }
}
