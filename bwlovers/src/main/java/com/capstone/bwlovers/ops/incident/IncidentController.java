package com.capstone.bwlovers.ops.incident;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;

import static com.capstone.bwlovers.ops.incident.IncidentJson.*;

@RestController
@RequestMapping("/internal/ai-incidents")
@ConditionalOnProperty(name = "incident.webhook.enabled", havingValue = "true")
public class IncidentController {
    private final IncidentJobStore store;
    private final IncidentWebhookSettings settings;

    IncidentController(IncidentJobStore store, IncidentWebhookSettings settings) { this.store = store; this.settings = settings; }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> receive(HttpServletRequest request) throws IOException {
        JsonNode body = body(request);
        Instant now = Instant.now();
        var events = GrafanaWebhookRequest.events(body, settings.job(), settings.pool(), now);
        JsonNode jobs = store.enqueue(events, now);
        ObjectNode result = object().put("ignoredResolved", body.path("alerts").size() - events.size());
        result.set("jobs", jobs);
        return ResponseEntity.accepted().body(result);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JsonNode> find(@PathVariable String jobId) {
        return store.find(jobId).<ResponseEntity<JsonNode>>map(job -> {
            job.remove("owner");
            if (job.hasNonNull("reportJson")) job.set("report", parse(job.remove("reportJson").asText()));
            return ResponseEntity.ok(IncidentInput.redact(job));
        }).orElseGet(() -> ResponseEntity.status(404).body(object().put("code", "JOB_NOT_FOUND_OR_EXPIRED")));
    }

    @PostMapping(path = "/{jobId}/retry", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> retry(@PathVariable String jobId, HttpServletRequest request) throws IOException {
        JsonNode body = body(request);
        fields(body, Set.of("deliveryChecked"));
        require(body.path("deliveryChecked").isBoolean() && body.path("deliveryChecked").asBoolean(),
                "Discord 채널의 기존 전송 여부를 확인한 뒤 deliveryChecked=true를 전달하세요.");
        return store.retry(jobId, Instant.now()) ? ResponseEntity.accepted().body(object().put("status", "QUEUED"))
                : ResponseEntity.status(409).body(object().put("code", "JOB_NOT_RETRYABLE_OR_QUEUE_FULL"));
    }

    private JsonNode body(HttpServletRequest request) throws IOException {
        byte[] data = request.getInputStream().readNBytes(64 * 1024 + 1);
        if (data.length > 64 * 1024) throw new IncidentBodyTooLargeException();
        return parse(new String(data, StandardCharsets.UTF_8));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<JsonNode> badRequest(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(object().put("code", "INVALID_ALERT").put("message", error.getMessage()));
    }

    @ExceptionHandler(IncidentBodyTooLargeException.class)
    ResponseEntity<JsonNode> tooLarge() { return ResponseEntity.status(413).body(object().put("code", "PAYLOAD_TOO_LARGE")); }

    @ExceptionHandler(IncidentQueueFullException.class)
    ResponseEntity<JsonNode> full() { return ResponseEntity.status(429).header("Retry-After", "60").body(object().put("code", "INCIDENT_QUEUE_FULL")); }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<JsonNode> unavailable() { return ResponseEntity.status(503).body(object().put("code", "INCIDENT_STORE_UNAVAILABLE")); }

    @ExceptionHandler(Exception.class)
    ResponseEntity<JsonNode> failed() {
        // Keep provider errors, Redis URLs and operational request bodies out of the global exception logger.
        return ResponseEntity.internalServerError().body(object().put("code", "INCIDENT_REQUEST_FAILED"));
    }
}

final class IncidentBodyTooLargeException extends RuntimeException { }
