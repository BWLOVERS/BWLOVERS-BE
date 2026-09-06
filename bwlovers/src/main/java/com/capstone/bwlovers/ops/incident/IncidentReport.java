package com.capstone.bwlovers.ops.incident;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.capstone.bwlovers.ops.incident.IncidentJson.*;

final class IncidentReport {
    static final String NOTICE = "AI 원인 후보입니다. 담당자가 근거를 확인한 뒤 수동으로 대응하세요. 자동 설정 변경은 수행하지 않습니다.";

    private IncidentReport() { }

    static ObjectNode create(JsonNode raw, JsonNode input, String mode, Set<String> omitted,
                             IncidentAnalyzer.Result result, Instant started, long duration, String promptVersion) {
        ObjectNode report = object().put("incidentId", raw.path("incidentId").asText())
                .put("sourceType", raw.path("sourceType").asText()).put("inputSha256", IncidentBenchmark.hash(raw))
                .put("mode", mode).put("promptVersion", promptVersion).put("model", result.model())
                .put("responseId", result.responseId()).put("analysisStartedAt", started.toString())
                .put("analysisCompletedAt", Instant.now().toString()).put("analysisDurationMs", duration)
                .put("humanReviewRequired", true).put("humanApprovalRequired", true).put("automaticChangesPerformed", false);
        report.set("omittedMetrics", MAPPER.valueToTree(omitted.stream().sorted().toList()));
        report.set("analyzedInput", input);
        report.set("diagnosis", result.diagnosis());
        report.set("usage", result.usage());
        if (raw.path("sourceType").asText().equals("LIVE")) report.set("measurementInput", raw);
        if (raw.hasNonNull("evidenceReference")) report.set("evidenceReference", raw.get("evidenceReference"));
        return (ObjectNode) IncidentInput.redact(report);
    }

    static Map<String, String> sections(JsonNode diagnosis) {
        Map<String, String> sections = new LinkedHashMap<>();
        sections.put("이상 현상", lines(diagnosis.path("anomalies")));
        sections.put("원인 후보", StreamSupport.stream(diagnosis.path("causeCandidates").spliterator(), false)
                .map(c -> "- " + c.path("code").asText() + ": " + c.path("summary").asText()).collect(Collectors.joining("\n")));
        sections.put("판단 근거", StreamSupport.stream(diagnosis.path("evidence").spliterator(), false)
                .map(e -> "- " + e.path("field").asText() + " = " + e.path("value").asText()
                        + " → " + e.path("interpretation").asText()).collect(Collectors.joining("\n")));
        sections.put("서비스 영향", diagnosis.path("serviceImpact").asText());
        sections.put("추가 확인 항목", lines(diagnosis.path("additionalChecks")));
        StringBuilder actions = new StringBuilder();
        int index = 1;
        for (JsonNode action : diagnosis.path("recommendedActions")) actions.append(index++).append(". ").append(action.asText()).append('\n');
        sections.put("권장 대응 순서", actions.toString().strip());
        sections.put("확신도", diagnosis.path("confidence").path("level").asText() + " — "
                + diagnosis.path("confidence").path("reason").asText());
        return sections;
    }

    static String markdown(JsonNode report) {
        StringBuilder text = new StringBuilder("# 장애 진단: ").append(report.path("incidentId").asText()).append("\n\n")
                .append(report.path("sourceType").asText()).append(" / ").append(report.path("mode").asText())
                .append(" / model: ").append(report.path("model").asText()).append("\n\n")
                .append(NOTICE).append("\n\n")
                .append("API 분석 시간: ").append(report.path("analysisDurationMs").asLong()).append(" ms. ")
                .append("담당자 검토를 포함한 전체 분석 시간은 별도로 측정합니다.\n");
        sections(report.path("diagnosis")).forEach((name, value) -> text.append("\n## ").append(name).append("\n\n").append(value).append('\n'));
        return IncidentInput.redactText(text.toString());
    }

    static ObjectNode discordPayload(JsonNode report) {
        ObjectNode payload = object();
        payload.putObject("allowed_mentions").putArray("parse");
        ObjectNode embed = payload.putArray("embeds").addObject();
        embed.put("title", clip("[" + report.path("sourceType").asText() + "] 장애 진단 " + report.path("incidentId").asText(), 220));
        embed.put("description", clip(NOTICE + "\n알림: " + report.at("/analyzedInput/alertName").asText("수동 분석")
                + "\n입력 모드: " + report.path("mode").asText()
                + "; 생략 지표: " + report.path("omittedMetrics").toString(), 500));
        embed.put("color", 0xE6A23C);
        var fields = embed.putArray("fields");
        sections(report.path("diagnosis")).forEach((name, value) -> fields.addObject()
                .put("name", name).put("value", clip(IncidentInput.redactText(value), 650)).put("inline", false));
        embed.putObject("footer").put("text", "긴 항목은 생략됩니다. 전체 근거는 보고서 파일 또는 인증된 작업 조회 API에서 확인하세요. 확신도는 정성 평가입니다.");
        return payload;
    }

    private static String lines(JsonNode values) {
        return StreamSupport.stream(values.spliterator(), false).map(value -> "- " + value.asText()).collect(Collectors.joining("\n"));
    }

    private static String clip(String text, int max) {
        if (text.length() <= max) return text;
        int end = max - 16;
        if (Character.isHighSurrogate(text.charAt(end - 1))) end--;
        return text.substring(0, end) + " …(전체: 로컬 보고서)";
    }
}
