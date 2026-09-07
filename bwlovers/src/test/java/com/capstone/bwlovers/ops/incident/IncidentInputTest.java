package com.capstone.bwlovers.ops.incident;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

import static com.capstone.bwlovers.ops.incident.IncidentFixtures.*;
import static com.capstone.bwlovers.ops.incident.IncidentJson.*;
import static org.junit.jupiter.api.Assertions.*;

class IncidentInputTest {
    @Test
    void allSimulationScenariosAreValidButHistoricalTemplateRequiresRealEvidence() throws Exception {
        for (String file : new String[]{"memory-exceeded", "instance-capacity", "db-pool", "request-latency"}) {
            var raw = read(Path.of("../docs/incident/scenarios/" + file + ".json"));
            assertEquals("SIMULATION", raw.path("sourceType").asText());
            assertDoesNotThrow(() -> IncidentInput.prepare(raw, "multi", Set.of()));
        }
        assertThrows(IllegalArgumentException.class, () -> IncidentInput.validate(read(Path.of("../docs/incident/historical-template.json"))));
    }

    @Test
    void rejectsUnknownFieldsAndInconsistentTimeWindows() {
        ObjectNode raw = input().put("expectedCause", "MEMORY_PRESSURE");
        assertThrows(IllegalArgumentException.class, () -> IncidentInput.prepare(raw, "multi", Set.of()));
        raw.remove("expectedCause");
        ((ObjectNode) raw.path("relatedLogs").get(0)).put("timestamp", "2026-08-01T10:05:00Z");
        assertThrows(IllegalArgumentException.class, () -> IncidentInput.prepare(raw, "multi", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> IncidentInput.validate(input().put("windowStart", "2026-08-01T11:00:00Z")));
    }

    @Test
    void rejectsNegativeFractionalAndInconsistentCountsButAllowsMissingMetrics() {
        for (var bad : new ObjectNode[]{input().put("requestCount", -1), input().put("requestCount", 1.5),
                input().put("requestCount", 2), input().put("dbMaxConnections", 0), input().put("p95Latency", "0.8")}) {
            assertThrows(IllegalArgumentException.class, () -> IncidentInput.validate(bad));
        }
        ObjectNode missing = input().putNull("memoryUsageMb");
        assertTrue(IncidentInput.prepare(missing, "multi", Set.of()).path("memoryUsageMb").isNull());
    }

    @Test
    void singleLogAndAblationActuallyRemoveEvidenceAndDoNotMutateOriginal() {
        var raw = input();
        raw.withArray("relatedLogs").addObject().put("timestamp", "2026-08-01T10:03:00Z").put("message", "Second signal");
        var single = IncidentInput.prepare(raw, "single-log", Set.of());
        assertEquals(1, single.path("relatedLogs").size());
        assertTrue(IncidentInput.METRICS.stream().noneMatch(single::has));
        assertFalse(single.has("sourceType"));
        assertFalse(single.has("incidentId"));
        var ablation = IncidentInput.prepare(raw, "multi", Set.of("memoryUsageMb", "memoryLimitMb"));
        assertFalse(ablation.has("memoryUsageMb"));
        assertTrue(ablation.has("requestCount"));
        assertTrue(raw.has("memoryUsageMb"));
        assertEquals(2, raw.path("relatedLogs").size());
        assertThrows(IllegalArgumentException.class, () -> IncidentInput.prepare(raw, "multi", Set.of("relatedLogs")));
    }

    @Test
    void masksCommonSensitiveValuesInLogsAndMetadata() {
        ObjectNode raw = input().put("service", "contact=operator@example.com");
        ((ObjectNode) raw.path("relatedLogs").get(0)).put("message",
                "Memory limit exceeded https://discord.com/api/webhooks/123/test-token ip=192.168.0.1 "
                        + "\"password\": \"with spaces\", token=secret123 Bearer eyJ-secret 010-1234-5678");
        String clean = IncidentInput.prepare(raw, "multi", Set.of()).toString();
        for (String secret : new String[]{"operator@example.com", "test-token", "192.168.0.1", "with spaces", "secret123", "eyJ-secret", "010-1234-5678"}) {
            assertFalse(clean.contains(secret), secret);
        }
        assertTrue(clean.contains("Memory limit exceeded"));
        assertTrue(clean.contains("[REDACTED]"));
    }

    @Test
    void rejectsDuplicateJsonFieldsAndTrailingObjectsWithoutEchoingContents() {
        for (String json : new String[]{"{\"password\":\"secret\",\"password\":1}", "{} {}"}) {
            var error = assertThrows(IllegalArgumentException.class, () -> parse(json));
            assertFalse(error.getMessage().contains("secret"));
        }
    }

    @Test
    void removesOAuthCodesAndDatabaseConnectionStringsAndChecksP95BaselineWindow() {
        String clean = IncidentInput.redactText("code=kakao-secret&state=keep jdbc:postgresql://user:password@db:5432/app redis://secret@cache:6379");
        for (String secret : new String[]{"kakao-secret", "user:password", "secret@cache"}) assertFalse(clean.contains(secret));
        assertTrue(clean.contains("state=keep"));
        var raw = input().put("baselineP95Latency", 0.4).put("baselineWindowStart", "2026-08-01T09:55:00Z")
                .put("baselineWindowEnd", "2026-08-01T10:00:00Z");
        assertDoesNotThrow(() -> IncidentInput.validate(raw));
        raw.put("baselineWindowEnd", "2026-08-01T09:59:59Z");
        assertThrows(IllegalArgumentException.class, () -> IncidentInput.validate(raw));
    }
}
