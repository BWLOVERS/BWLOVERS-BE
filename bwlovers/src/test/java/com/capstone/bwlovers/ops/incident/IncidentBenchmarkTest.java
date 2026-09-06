package com.capstone.bwlovers.ops.incident;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static com.capstone.bwlovers.ops.incident.IncidentFixtures.*;
import static com.capstone.bwlovers.ops.incident.IncidentJson.*;
import static org.junit.jupiter.api.Assertions.*;

class IncidentBenchmarkTest {
    @TempDir Path temp;

    @Test
    void pairsSameInputAndTrialAndExcludesUnpairedMeasurements() throws Exception {
        for (int trial = 1; trial <= 3; trial++) {
            measurement("same", trial, "MANUAL", 900_000);
            measurement("same", trial, "AI_ASSISTED", 180_000);
        }
        measurement("different", 1, "AI_ASSISTED", 1_000);
        JsonNode summary = IncidentBenchmark.summarize(temp);
        assertEquals(3, summary.at("/overall/pairedTrials").asInt());
        assertEquals(1, summary.at("/overall/unpairedTrials").asInt());
        assertEquals(900.0, summary.at("/overall/manualMeanSeconds").asDouble());
        assertEquals(180.0, summary.at("/overall/aiAssistedMeanSeconds").asDouble());
        assertEquals(80.0, summary.at("/overall/reductionPercent").asDouble(), 0.0001);
    }

    @Test
    void emptyDataHasNoInventedImprovementAndDuplicateTrialsFail() throws Exception {
        assertTrue(IncidentBenchmark.summarize(temp).at("/overall/reductionPercent").isNull());
        measurement("same", 1, "MANUAL", 2000);
        JsonNode original = read(temp.resolve("same-1-MANUAL.measurement.json"));
        writeNew(temp.resolve("duplicate.measurement.json"), original);
        assertThrows(IllegalArgumentException.class, () -> IncidentBenchmark.summarize(temp));
    }

    @Test
    void timerRequiresActualReviewNotesAndCannotFinishTwice() throws Exception {
        Path input = temp.resolve("input.json"), session = temp.resolve("session.json"), notes = temp.resolve("notes.json");
        writeNew(input, input());
        IncidentBenchmark.start(input, "MANUAL", 1, session);
        writeNew(notes, parse("{\"causeCandidates\":[\"메모리 압박 가능성\"],\"additionalChecks\":[\"시간대 확인\"]}"));
        Instant ended = IncidentInput.instant(read(session).path("startedAt").asText()).plusSeconds(2);
        JsonNode result = read(IncidentBenchmark.stop(session, notes, ended));
        assertTrue(result.path("elapsedMs").asLong() > 0);
        assertEquals(IncidentBenchmark.hash(input()), result.path("inputSha256").asText());
        assertThrows(java.nio.file.FileAlreadyExistsException.class, () -> IncidentBenchmark.stop(session, notes, ended));
    }

    @Test
    void assistedTimingRequiresFreshFullInputAiReportAndSameCase() throws Exception {
        Path input = temp.resolve("input.json"), session = temp.resolve("session.json"), notes = temp.resolve("notes.json");
        writeNew(input, input());
        IncidentBenchmark.start(input, "AI_ASSISTED", 1, session);
        writeNew(notes, parse("{\"causeCandidates\":[\"메모리 압박\"],\"additionalChecks\":[\"종료 로그 확인\"],\"diagnosisReport\":\"run/report.json\"}"));
        IncidentDiagnosisCli.run(new String[]{"analyze", "--input", input.toString(), "--output", temp.resolve("run").toString()},
                Map.of("OPENAI_API_KEY", "test"), (u, h, b) -> response(diagnosis()));
        assertEquals("AI_ASSISTED", read(IncidentBenchmark.stop(session, notes)).path("method").asText());
        Path secondSession = temp.resolve("session2.json");
        IncidentBenchmark.start(input, "AI_ASSISTED", 2, secondSession);
        assertThrows(IllegalArgumentException.class, () -> IncidentBenchmark.stop(secondSession, notes));
    }

    @Test
    void inputFingerprintIgnoresObjectKeyOrderButTracksValues() {
        assertEquals(IncidentBenchmark.hash(parse("{\"a\":1,\"b\":2}")), IncidentBenchmark.hash(parse("{\"b\":2,\"a\":1}")));
        assertNotEquals(IncidentBenchmark.hash(input()), IncidentBenchmark.hash(input().put("memoryUsageMb", 100)));
    }

    @Test
    void medianResistsOutlierAndDifferentMeasurementScopesAreNeverPooled() throws Exception {
        measurement("same", 1, "MANUAL", 600_000);
        measurement("same", 2, "MANUAL", 720_000);
        measurement("same", 3, "MANUAL", 7_200_000);
        for (int i = 1; i <= 3; i++) measurement("same", i, "AI_ASSISTED", 240_000);
        JsonNode summary = IncidentBenchmark.summarize(temp);
        assertEquals(720, summary.at("/overall/manualMedianSeconds").asDouble());
        assertEquals(240, summary.at("/overall/aiAssistedMedianSeconds").asDouble());
        assertEquals(66.666666, summary.at("/overall/reductionPercent").asDouble(), 0.00001);
        assertNotEquals(summary.at("/overall/reductionPercent"), summary.at("/overall/meanReductionPercent"));
        ObjectNode review = (ObjectNode) read(temp.resolve("same-1-MANUAL.measurement.json"));
        review.put("measurementScope", "REVIEW_ONLY");
        writeNew(temp.resolve("review.measurement.json"), review);
        assertTrue(IncidentBenchmark.summarize(temp).path("overall").isNull());
    }

    @Test
    void reviewOnlyTimingUsesAnAlreadyCompletedReport() throws Exception {
        Path input = temp.resolve("input.json"), session = temp.resolve("review.json"), notes = temp.resolve("notes.json");
        writeNew(input, input());
        IncidentDiagnosisCli.run(new String[]{"analyze", "--input", input.toString(), "--output", temp.resolve("run").toString()},
                Map.of("OPENAI_API_KEY", "test"), (u, h, b) -> response(diagnosis()));
        IncidentBenchmark.start(input, "AI_ASSISTED", 1, session, "REVIEW_ONLY");
        writeNew(notes, parse("{\"causeCandidates\":[\"메모리 압박\"],\"additionalChecks\":[\"로그 확인\"],\"diagnosisReport\":\"run/report.json\"}"));
        Instant ended = IncidentInput.instant(read(session).path("startedAt").asText()).plusSeconds(2);
        assertEquals("REVIEW_ONLY", read(IncidentBenchmark.stop(session, notes, ended)).path("measurementScope").asText());
    }

    private void measurement(String variant, int trial, String method, long elapsedMs) throws Exception {
        ObjectNode record = object().put("incidentId", "sim-memory").put("sourceType", "SIMULATION")
                .put("inputSha256", IncidentBenchmark.hash(input().put("service", variant)))
                .put("trial", trial).put("method", method).put("elapsedMs", elapsedMs);
        writeNew(temp.resolve(variant + "-" + trial + "-" + method + ".measurement.json"), record);
    }
}
