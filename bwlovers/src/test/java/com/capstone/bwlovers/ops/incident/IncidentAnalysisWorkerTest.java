package com.capstone.bwlovers.ops.incident;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.capstone.bwlovers.ops.incident.IncidentFixtures.*;
import static com.capstone.bwlovers.ops.incident.IncidentJson.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IncidentAnalysisWorkerTest {
    @Test
    void checkpointsDiagnosisBeforeDiscordAndResendDoesNotCallAi() throws Exception {
        IncidentJobStore store = mock(IncidentJobStore.class);
        List<ObjectNode> saved = new ArrayList<>();
        when(store.save(any(), any())).thenAnswer(call -> { saved.add(((ObjectNode) call.getArgument(0)).deepCopy()); return true; });
        AtomicInteger ai = new AtomicInteger(), discord = new AtomicInteger();
        IncidentHttp http = (uri, headers, body) -> {
            if (uri.getHost().equals("api.openai.com")) { ai.incrementAndGet(); return response(diagnosis()); }
            discord.incrementAndGet();
            assertTrue(saved.get(saved.size() - 1).hasNonNull("reportJson"));
            throw new IOException("delivery timeout");
        };
        ObjectNode job = object().put("jobId", "a".repeat(64)).put("owner", "test");
        job.set("event", PrometheusIncidentCollectorTest.event());
        try (var worker = new IncidentAnalysisWorker(store, event -> input(), new IncidentAnalyzer(http, "test", "gpt-4.1-mini"),
                http, IncidentDiscord.webhook("https://discord.com/api/webhooks/1/test"))) {
            worker.process(job);
            assertEquals(List.of("ANALYZED", "DELIVERING", "DELIVERY_UNKNOWN"), saved.stream().map(j -> j.path("status").asText()).toList());
            assertTrue(parse(job.path("reportJson").asText()).path("humanApprovalRequired").asBoolean());
            worker.process(job);
            assertEquals(1, ai.get());
            assertEquals(2, discord.get());
        }
    }

    @Test
    void invalidAiOutputNeverNotifiesAndLostLeaseStopsSideEffects() throws Exception {
        IncidentJobStore store = mock(IncidentJobStore.class);
        when(store.save(any(), any())).thenReturn(true);
        AtomicInteger discord = new AtomicInteger();
        IncidentHttp http = (uri, headers, body) -> {
            if (uri.getHost().equals("discord.com")) { discord.incrementAndGet(); return object().put("id", "123"); }
            return response(object());
        };
        ObjectNode job = object().put("jobId", "a".repeat(64)).put("owner", "test");
        try (var worker = new IncidentAnalysisWorker(store, event -> input(), new IncidentAnalyzer(http, "test", "gpt-4.1-mini"),
                http, IncidentDiscord.webhook("https://discord.com/api/webhooks/1/test"))) {
            worker.process(job);
            assertEquals("FAILED", job.path("status").asText());
            assertEquals(0, discord.get());
        }
        when(store.save(any(), any())).thenReturn(false);
        job.put("reportJson", object().set("diagnosis", diagnosis()).toString());
        try (var worker = new IncidentAnalysisWorker(store, event -> { fail("must reuse report"); return null; },
                new IncidentAnalyzer(http, "test", "gpt-4.1-mini"), http, IncidentDiscord.webhook("https://discord.com/api/webhooks/1/test"))) {
            worker.process(job);
            assertEquals(0, discord.get());
        }
    }
}
