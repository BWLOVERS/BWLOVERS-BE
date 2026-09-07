package com.capstone.bwlovers.ops.incident;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.capstone.bwlovers.ops.incident.IncidentJson.*;
import static org.junit.jupiter.api.Assertions.*;

class RedisIncidentJobStoreTest {
    @TempDir static Path temp;
    static Process process;
    static LettuceConnectionFactory factory;
    static StringRedisTemplate redis;

    @BeforeAll static void startRedis() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        try {
            process = new ProcessBuilder("redis-server", "--port", Integer.toString(port), "--bind", "127.0.0.1",
                    "--save", "", "--appendonly", "no", "--dir", temp.toString())
                    .redirectErrorStream(true).redirectOutput(temp.resolve("redis.log").toFile()).start();
        } catch (IOException missing) {
            Assumptions.abort("redis-server executable is required for Redis integration tests"); return;
        }
        factory = new LettuceConnectionFactory("127.0.0.1", port);
        factory.afterPropertiesSet(); factory.start();
        redis = new StringRedisTemplate(factory);
        redis.afterPropertiesSet();
        for (int attempt = 0; attempt < 50; attempt++) {
            try (var connection = factory.getConnection()) { assertEquals("PONG", connection.ping()); return; }
            catch (RuntimeException unavailable) { Thread.sleep(20); }
        }
        fail("Test Redis did not start");
    }

    @AfterAll static void stopRedis() throws Exception {
        if (factory != null) factory.destroy();
        if (process != null) { process.destroy(); if (!process.waitFor(3, TimeUnit.SECONDS)) process.destroyForcibly(); }
    }

    RedisIncidentJobStore store(int capacity) {
        return new RedisIncidentJobStore(redis, "test:{incident}:" + System.nanoTime() + ":", capacity);
    }

    @Test void concurrentDeliveryCreatesExactlyOneQueuedJobAndOneOwner() throws Exception {
        var store = store(10);
        var executor = Executors.newFixedThreadPool(8);
        Instant now = Instant.now();
        try {
            List<Future<JsonNode>> submitted = new ArrayList<>();
            for (int i = 0; i < 8; i++) submitted.add(executor.submit(() -> store.enqueue(List.of(PrometheusIncidentCollectorTest.event()), now)));
            int created = 0;
            for (var future : submitted) if (future.get().at("/0/created").asBoolean()) created++;
            assertEquals(1, created);
            assertTrue(store.claim(now).isPresent());
            assertTrue(store.claim(now).isEmpty());
        } finally { executor.shutdownNow(); }
    }

    @Test void groupedEnqueueIsAtomicWhenQueueIsFullAndCompletedDuplicatesStillWork() {
        var store = store(1);
        Instant now = Instant.now();
        ObjectNode a = PrometheusIncidentCollectorTest.event(), b = a.deepCopy().put("jobId", "b".repeat(64));
        assertThrows(IncidentQueueFullException.class, () -> store.enqueue(List.of(a, b), now));
        assertTrue(store.find(a.path("jobId").asText()).isEmpty());
        store.enqueue(List.of(a), now);
        assertFalse(store.enqueue(List.of(a), now).at("/0/created").asBoolean());
        assertThrows(IncidentQueueFullException.class, () -> store.enqueue(List.of(b), now));
    }

    @Test void expiredOwnerCannotOverwriteNewWorkerAndOpaqueReportSurvivesLua() {
        var store = store(10);
        Instant now = Instant.now();
        store.enqueue(List.of(PrometheusIncidentCollectorTest.event()), now);
        ObjectNode first = store.claim(now).orElseThrow();
        first.put("reportJson", "{\"relatedLogs\":[],\"omittedMetrics\":[]}").put("status", "ANALYZED");
        assertTrue(store.save(first, now));
        Instant recoveredAt = now.plusMillis(RedisIncidentJobStore.LEASE_MILLIS + 1);
        ObjectNode second = store.claim(recoveredAt).orElseThrow();
        assertEquals(2, second.path("attempts").asInt());
        assertFalse(store.save(first.put("status", "SENT"), recoveredAt));
        assertTrue(parse(second.path("reportJson").asText()).path("relatedLogs").isArray());
        assertTrue(store.save(second.put("status", "SENT"), recoveredAt));
        assertTrue(store.claim(recoveredAt.plusSeconds(10000)).isEmpty());
        assertFalse(store.retry(first.path("jobId").asText(), recoveredAt));
    }

    @Test void interruptedDiscordDeliveryRequiresExplicitRetryAndReusesReport() {
        var store = store(10);
        Instant now = Instant.now();
        store.enqueue(List.of(PrometheusIncidentCollectorTest.event()), now);
        ObjectNode job = store.claim(now).orElseThrow();
        job.put("reportJson", "{\"saved\":true}").put("status", "DELIVERING");
        assertTrue(store.save(job, now));
        Instant afterLease = now.plusMillis(RedisIncidentJobStore.LEASE_MILLIS + 1);
        assertTrue(store.claim(afterLease).isEmpty());
        String id = job.path("jobId").asText();
        assertEquals("DELIVERY_UNKNOWN", store.find(id).orElseThrow().path("status").asText());
        assertTrue(store.retry(id, afterLease));
        assertEquals("{\"saved\":true}", store.claim(afterLease).orElseThrow().path("reportJson").asText());
    }
}
