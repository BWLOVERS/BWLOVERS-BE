package com.capstone.bwlovers.ops.incident;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.capstone.bwlovers.ops.incident.IncidentJson.*;

final class IncidentAnalysisWorker implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(IncidentAnalysisWorker.class);
    private final IncidentJobStore store;
    private final IncidentContextCollector collector;
    private final IncidentAnalyzer analyzer;
    private final IncidentHttp http;
    private final URI webhook;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "incident-worker"); thread.setDaemon(true); return thread;
    });

    IncidentAnalysisWorker(IncidentJobStore store, IncidentContextCollector collector, IncidentAnalyzer analyzer,
                           IncidentHttp http, URI webhook) {
        this.store = store; this.collector = collector; this.analyzer = analyzer; this.http = http; this.webhook = webhook;
    }

    void start() { executor.scheduleWithFixedDelay(this::poll, 1, 5, TimeUnit.SECONDS); }

    void poll() {
        try { store.claim(Instant.now()).ifPresent(this::process); }
        catch (RuntimeException e) { log.warn("Incident queue unavailable; next poll will retry."); }
    }

    void process(ObjectNode job) {
        boolean delivering = false;
        try {
            if (!job.hasNonNull("reportJson")) {
                ObjectNode raw = collector.collect(job.path("event"));
                ObjectNode input = IncidentInput.prepare(raw, "multi", Set.of());
                Instant started = Instant.now();
                long tick = System.nanoTime();
                IncidentAnalyzer.Result result = analyzer.analyze(input);
                // Keep report JSON opaque to Redis Lua: cjson otherwise converts empty arrays to objects.
                job.put("reportJson", IncidentReport.create(raw, input, "multi", Set.of(), result, started,
                        (System.nanoTime() - tick) / 1_000_000, analyzer.promptVersion()).toString());
                job.put("status", "ANALYZED");
                if (!store.save(job, Instant.now())) return;
            }
            job.put("status", "DELIVERING");
            if (!store.save(job, Instant.now())) return;
            delivering = true;
            String messageId = IncidentDiscord.send(http, webhook, parse(job.path("reportJson").asText()));
            job.put("status", "SENT").put("messageId", messageId).put("completedAt", Instant.now().toString());
            store.save(job, Instant.now());
        } catch (Exception e) {
            job.put("status", delivering ? "DELIVERY_UNKNOWN" : "FAILED");
            job.put("failureReason", e instanceof IncidentHttp.Failure ? e.getMessage() : "수집·분석·저장 또는 전송 실패. 민감한 오류 본문은 기록하지 않습니다.");
            try { store.save(job, Instant.now()); }
            catch (RuntimeException unavailable) { log.warn("Incident job checkpoint unavailable; lease recovery will handle job {}.", job.path("jobId").asText()); }
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Incident job {} stopped with status {}.", job.path("jobId").asText(), job.path("status").asText());
        }
    }

    @Override public void close() { executor.shutdownNow(); }
}
