package com.capstone.bwlovers.ops.incident;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.capstone.bwlovers.ops.incident.IncidentFixtures.*;
import static com.capstone.bwlovers.ops.incident.IncidentJson.*;
import static org.junit.jupiter.api.Assertions.*;

class IncidentDiagnosisCliTest {
    @TempDir Path temp;
    private static final Map<String, String> ENV = Map.of("OPENAI_API_KEY", "test-key",
            "DISCORD_INCIDENT_WEBHOOK_URL", "https://discord.com/api/webhooks/123/test-token");

    @Test
    void dryRunRequiresNoCredentialsAndMakesNoNetworkCalls() throws Exception {
        Path input = temp.resolve("input.json"), output = temp.resolve("dry");
        writeNew(input, input());
        IncidentDiagnosisCli.run(new String[]{"analyze", "--input", input.toString(), "--output", output.toString(), "--dry-run"},
                Map.of(), (u, h, b) -> { fail("dry-run must be offline"); return null; });
        assertTrue(Files.exists(output.resolve("request.json")));
        assertFalse(Files.exists(output.resolve("report.json")));
    }

    @Test
    void completeAnalysisIsSavedWithoutSendingByDefaultAndCanBeEvaluated() throws Exception {
        Path input = temp.resolve("input.json"), output = temp.resolve("run");
        writeNew(input, input());
        AtomicInteger calls = new AtomicInteger();
        IncidentDiagnosisCli.run(new String[]{"analyze", "--input", input.toString(), "--output", output.toString()}, ENV,
                (uri, headers, body) -> { calls.incrementAndGet(); assertEquals("api.openai.com", uri.getHost()); return response(diagnosis()); });
        assertEquals(1, calls.get());
        JsonNode report = read(output.resolve("report.json"));
        assertTrue(report.path("humanReviewRequired").asBoolean());
        assertFalse(report.path("automaticChangesPerformed").asBoolean(true));
        assertEquals("SIMULATION", report.path("sourceType").asText());
        assertTrue(Files.exists(output.resolve("report.md")));
        assertEquals(0, read(output.resolve("discord-preview.json")).at("/allowed_mentions/parse").size());
        assertFalse(Files.exists(output.resolve("delivery.json")));
        JsonNode evaluation = IncidentEvaluation.evaluate(temp, Path.of("../docs/incident/expectations.json"));
        assertTrue(evaluation.at("/cases/0/allExpectedCandidatesPresent").asBoolean());
        assertEquals(3, evaluation.path("unrunCases").size());
    }

    @Test
    void failedDiscordDeliveryPreservesDiagnosisAndCanBeResentWithoutAnotherAiCall() throws Exception {
        Path input = temp.resolve("input.json"), output = temp.resolve("run");
        writeNew(input, input());
        AtomicInteger aiCalls = new AtomicInteger();
        assertThrows(IOException.class, () -> IncidentDiagnosisCli.run(new String[]{"analyze", "--input", input.toString(),
                "--output", output.toString(), "--send-discord"}, ENV, (uri, headers, body) -> {
            if (uri.getHost().equals("api.openai.com")) { aiCalls.incrementAndGet(); return response(diagnosis()); }
            assertTrue(Files.exists(output.resolve("report.json")));
            assertEquals("wait=true", uri.getQuery());
            assertTrue(headers.isEmpty());
            throw new IOException("test outage");
        }));
        assertEquals("FAILED_OR_UNKNOWN", read(output.resolve("delivery.json")).path("status").asText());
        Path receipt = temp.resolve("resent.json");
        IncidentHttp sender = (uri, headers, body) -> {
            assertEquals("discord.com", uri.getHost());
            assertTrue(body.at("/allowed_mentions/parse").isEmpty());
            return object().put("id", "987654");
        };
        String[] args = {"send", "--report", output.resolve("report.json").toString(), "--output", receipt.toString()};
        IncidentDiagnosisCli.run(args, ENV, sender);
        assertEquals("SENT", read(receipt).path("status").asText());
        assertEquals(1, aiCalls.get());
        assertThrows(java.nio.file.FileAlreadyExistsException.class,
                () -> IncidentDiagnosisCli.run(args, ENV, (u, h, b) -> { fail("existing receipt must block send"); return null; }));
    }

    @Test
    void malformedAiResponseNeverSendsDiscordAndExistingOutputNeverCallsAi() throws Exception {
        Path input = temp.resolve("input.json"), output = temp.resolve("run");
        writeNew(input, input());
        AtomicInteger calls = new AtomicInteger();
        String[] args = {"analyze", "--input", input.toString(), "--output", output.toString(), "--send-discord"};
        assertThrows(IllegalArgumentException.class, () -> IncidentDiagnosisCli.run(args, ENV,
                (u, h, b) -> { calls.incrementAndGet(); return response(diagnosis()).put("status", "incomplete"); }));
        assertEquals(1, calls.get());
        assertFalse(Files.exists(output.resolve("report.json")));
        assertThrows(java.nio.file.FileAlreadyExistsException.class,
                () -> IncidentDiagnosisCli.run(args, ENV, (u, h, b) -> { fail("must preserve existing output"); return null; }));
    }

    @Test
    void discordPayloadFitsLimitsWithAllSevenSectionsAndDisablesMentions() {
        ObjectNode d = diagnosis();
        d.put("serviceImpact", "@everyone " + "긴 영향 😀".repeat(600));
        ObjectNode report = object().put("incidentId", "test").put("sourceType", "SIMULATION").put("mode", "multi");
        report.putArray("omittedMetrics");
        report.set("diagnosis", d);
        JsonNode payload = IncidentReport.discordPayload(report);
        assertTrue(payload.at("/allowed_mentions/parse").isEmpty());
        JsonNode embed = payload.at("/embeds/0");
        assertEquals(7, embed.path("fields").size());
        int total = embed.path("title").asText().length() + embed.path("description").asText().length()
                + embed.at("/footer/text").asText().length();
        for (JsonNode field : embed.path("fields")) {
            assertTrue(field.path("value").asText().length() <= 1024);
            total += field.path("name").asText().length() + field.path("value").asText().length();
        }
        assertTrue(total <= 6000);
    }

    @Test
    void rejectsDangerousWebhookDestinationsAndInvalidOptions() {
        for (String value : new String[]{"http://discord.com/api/webhooks/1/token", "https://evil.test/api/webhooks/1/token",
                "https://discord.com@evil.test/api/webhooks/1/token", "https://discord.com/api/webhooks/1/token?wait=false"}) {
            assertThrows(IllegalArgumentException.class, () -> IncidentDiscord.webhook(value));
        }
        assertThrows(IllegalArgumentException.class, () -> IncidentDiagnosisCli.run(new String[]{"analyze", "--send-discord", "--dry-run"}, ENV, null));
        assertThrows(IllegalArgumentException.class, () -> IncidentDiagnosisCli.run(new String[]{"analyze", "--typo", "x"}, ENV, null));
    }
}
