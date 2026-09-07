package com.capstone.bwlovers.ops.incident;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.capstone.bwlovers.ops.incident.IncidentJson.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GrafanaWebhookRequest(String status, Map<String, String> commonLabels,
                                    Map<String, String> commonAnnotations, List<GrafanaAlert> alerts) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GrafanaAlert(String status, Map<String, String> labels, Map<String, String> annotations,
                               Map<String, Double> values, String startsAt, String fingerprint) { }

    static List<ObjectNode> events(JsonNode body, String expectedJob, String defaultPool, Instant receivedAt) {
        require(body.isObject() && Set.of("firing", "resolved").contains(body.path("status").asText()), "Grafana status가 올바르지 않습니다.");
        require(!body.has("truncatedAlerts") || body.path("truncatedAlerts").isIntegralNumber()
                && body.path("truncatedAlerts").asInt() == 0, "잘린 알림은 수신하지 않습니다. Grafana Max Alerts 설정을 확인하세요.");
        require(body.path("alerts").isArray() && !body.path("alerts").isEmpty() && body.path("alerts").size() <= 20,
                "알림은 요청당 1~20건이어야 합니다.");
        List<ObjectNode> events = new ArrayList<>();
        for (JsonNode alert : body.path("alerts")) {
            String status = text(alert, "status", 10);
            require(Set.of("firing", "resolved").contains(status), "개별 알림 status가 올바르지 않습니다.");
            if (status.equals("resolved")) continue;
            require(body.path("status").asText().equals("firing"), "그룹과 개별 알림 status가 모순됩니다.");
            JsonNode labels = alert.path("labels");
            require(labels.isObject() && labels.size() <= 50, "알림 labels가 올바르지 않습니다.");
            labels.fields().forEachRemaining(entry -> require(entry.getKey().length() <= 100
                    && entry.getValue().isTextual() && entry.getValue().asText().length() <= 300, "알림 label 길이/타입이 올바르지 않습니다."));
            String job = text(labels, "job", 100), instance = text(labels, "instance", 200);
            require(job.equals(expectedJob), "수집 대상으로 등록되지 않은 Prometheus job입니다.");
            require(instance.matches("[A-Za-z0-9._:/\\[\\]-]{1,200}"), "instance label 형식이 올바르지 않습니다.");
            String pool = labels.has("pool") ? text(labels, "pool", 100) : defaultPool;
            require(pool.matches("[A-Za-z0-9._-]{1,100}"), "pool label 형식이 올바르지 않습니다.");
            Instant starts = IncidentInput.instant(text(alert, "startsAt", 40));
            require(!starts.isAfter(receivedAt.plusSeconds(60)), "미래에 시작하는 알림은 수신하지 않습니다.");
            ObjectNode identity = object().put("startsAt", starts.toString());
            identity.set("labels", labels);
            ObjectNode event = object().put("jobId", IncidentBenchmark.hash(identity)).put("alertName", text(labels, "alertname", 150))
                    .put("job", job).put("instance", instance).put("pool", pool).put("startsAt", starts.toString())
                    .put("receivedAt", receivedAt.toString());
            // URLs, annotation commands and Grafana expression IDs/values never become metric queries or logs.
            events.add(event);
        }
        return events;
    }
}
