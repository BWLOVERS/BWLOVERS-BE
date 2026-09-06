package com.capstone.bwlovers.ops.incident;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalDouble;

import static com.capstone.bwlovers.ops.incident.IncidentJson.*;

final class PrometheusIncidentCollector implements IncidentContextCollector {
    @FunctionalInterface interface Query {
        OptionalDouble execute(String expression, Instant at) throws IOException, InterruptedException;
    }

    private final Query query;

    PrometheusIncidentCollector(Query query) { this.query = query; }

    @Override public ObjectNode collect(JsonNode event) throws InterruptedException {
        Instant end = IncidentInput.instant(event.path("receivedAt").asText()).truncatedTo(ChronoUnit.SECONDS);
        Instant start = end.minusSeconds(300);
        ObjectNode raw = object().put("incidentId", "grafana-" + event.path("jobId").asText()).put("sourceType", "LIVE")
                .put("service", event.path("job").asText()).put("revision", "UNKNOWN")
                .put("alertName", event.path("alertName").asText()).put("alertStartedAt", event.path("startsAt").asText())
                .put("windowStart", start.toString()).put("windowEnd", end.toString())
                .put("baselineWindowStart", start.minusSeconds(300).toString()).put("baselineWindowEnd", start.toString())
                .put("countValuesEstimated", true)
                .put("metricScope", "수신 직전 5분, 단일 Prometheus job/instance 대상. 요청=increase 추정치, 지연=구간 histogram P95, "
                        + "JVM heap=구간 총사용량 최대/유효 풀 한도 최소 MiB. DB=같은 pool의 구간 active 최대/max 최소. "
                        + "최댓값 동시성 보장 없음. 리비전 식별 불가. target=" + event.path("instance").asText());
        raw.putArray("relatedLogs");
        var notes = raw.putArray("collectionNotes");
        notes.add("Cloud Logging 미연동: 관련 로그 미수집. 컨테이너 메모리와 Cloud Run 인스턴스 수/한도 미수집.");
        notes.add("HTTP 지표는 애플리케이션에 도달한 요청만 포함. 플랫폼에서 거절된 Cloud Run 429는 포함되지 않을 수 있음.");
        for (String key : IncidentInput.METRICS) raw.putNull(key);
        for (var entry : expressions(event).entrySet()) {
            try {
                OptionalDouble value = query.execute(entry.getValue(), end.minusMillis(1));
                if (value.isPresent()) {
                    double number = value.getAsDouble();
                    if (entry.getKey().equals("dbActiveConnections") || entry.getKey().equals("dbMaxConnections")) {
                        require(number == Math.rint(number) && number < Long.MAX_VALUE, "DB 연결 수가 정수가 아닙니다.");
                        raw.put(entry.getKey(), (long) number);
                    } else raw.put(entry.getKey(), number);
                } else notes.add(entry.getKey() + ": 데이터 없음/NaN. 0으로 대체하지 않음.");
            } catch (IOException | IllegalArgumentException e) {
                notes.add(entry.getKey() + ": 조회 실패 또는 범위가 모호한 시계열. 미수집 처리.");
            }
        }
        raw = (ObjectNode) IncidentInput.redact(raw);
        IncidentInput.validate(raw);
        return raw;
    }

    static Map<String, String> expressions(JsonNode event) {
        String labels = "job=" + event.path("job").toString() + ",instance=" + event.path("instance").toString();
        String requests = labels + ",uri!~\"/actuator/.*|/internal/.*\"";
        String p95 = "histogram_quantile(0.95,sum by(le)(rate(http_server_requests_seconds_bucket{" + requests + "}[5m]%s)))";
        String heapUsed = "jvm_memory_used_bytes{" + labels + ",area=\"heap\"}";
        String heapMax = "jvm_memory_max_bytes{" + labels + ",area=\"heap\"}";
        Map<String, String> queries = new LinkedHashMap<>();
        queries.put("requestCount", "sum(increase(http_server_requests_seconds_count{" + requests + "}[5m]))");
        queries.put("status429Count", "sum(increase(http_server_requests_seconds_count{" + requests + ",status=\"429\"}[5m]))");
        queries.put("status5xxCount", "sum(increase(http_server_requests_seconds_count{" + requests + ",status=~\"5..\"}[5m]))");
        queries.put("p95Latency", p95.formatted(""));
        queries.put("baselineP95Latency", p95.formatted(" offset 5m"));
        queries.put("jvmHeapUsageMb", "max_over_time((sum(" + heapUsed + " and on(job,instance,id) (" + heapMax + " > 0)))[5m:15s])/1048576");
        queries.put("jvmHeapLimitMb", "min_over_time((sum(" + heapMax + " > 0))[5m:15s])/1048576");
        String pool = labels + ",pool=" + event.path("pool").toString();
        queries.put("dbActiveConnections", "max_over_time(hikaricp_connections_active{" + pool + "}[5m])");
        queries.put("dbMaxConnections", "min_over_time(hikaricp_connections_max{" + pool + "}[5m])");
        return queries;
    }

    static Query http(URI baseUrl, String bearerToken) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
        String base = baseUrl.toString().replaceAll("/+$", "");
        return (expression, at) -> {
            URI uri = URI.create(base + "/api/v1/query?query=" + URLEncoder.encode(expression, StandardCharsets.UTF_8)
                    + "&time=" + URLEncoder.encode(at.toString(), StandardCharsets.UTF_8) + "&timeout=5s");
            HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET();
            if (bearerToken != null && !bearerToken.isBlank()) request.header("Authorization", "Bearer " + bearerToken);
            HttpResponse<String> response;
            try { response = client.send(request.build(), HttpResponse.BodyHandlers.ofString()); }
            catch (IOException e) { throw new IOException("Prometheus 조회 실패"); }
            require(response.statusCode() == 200 && response.body().length() <= MAX_FILE_BYTES, "Prometheus 응답 실패");
            return value(parse(response.body()));
        };
    }

    static OptionalDouble value(JsonNode response) {
        require(response.path("status").asText().equals("success") && response.at("/data/resultType").asText().equals("vector"), "Prometheus 응답 타입이 올바르지 않습니다.");
        require(!response.has("warnings") || response.path("warnings").isEmpty(), "Prometheus가 부분 결과 경고를 반환했습니다.");
        JsonNode results = response.at("/data/result");
        require(results.isArray() && results.size() <= 1, "여러 시계열을 하나의 관측값으로 합칠 수 없습니다.");
        if (results.isEmpty()) return OptionalDouble.empty();
        JsonNode sample = results.get(0).path("value");
        require(sample.isArray() && sample.size() == 2 && sample.get(1).isTextual(), "Prometheus sample 형식이 올바르지 않습니다.");
        double number;
        try { number = Double.parseDouble(sample.get(1).asText()); }
        catch (NumberFormatException e) { return OptionalDouble.empty(); }
        return Double.isFinite(number) && number >= 0 ? OptionalDouble.of(number) : OptionalDouble.empty();
    }
}

@FunctionalInterface
interface IncidentContextCollector {
    ObjectNode collect(JsonNode event) throws IOException, InterruptedException;
}
