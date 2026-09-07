package com.capstone.bwlovers.ops.incident;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.capstone.bwlovers.ops.incident.IncidentJson.*;

final class IncidentBenchmark {
    private IncidentBenchmark() { }

    static String hash(JsonNode input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical(input).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private static JsonNode canonical(JsonNode input) {
        if (input.isObject()) {
            ObjectNode sorted = object();
            List<String> keys = new ArrayList<>();
            input.fieldNames().forEachRemaining(keys::add);
            keys.stream().sorted().forEach(key -> sorted.set(key, canonical(input.get(key))));
            return sorted;
        }
        if (input.isArray()) {
            var values = MAPPER.createArrayNode();
            input.forEach(value -> values.add(canonical(value)));
            return values;
        }
        return input;
    }

    static void start(Path inputFile, String method, int trial, Path session) throws IOException {
        start(inputFile, method, trial, session, "END_TO_END");
    }

    static void start(Path inputFile, String method, int trial, Path session, String scope) throws IOException {
        JsonNode input = read(inputFile);
        IncidentInput.validate(input);
        require(Set.of("MANUAL", "AI_ASSISTED").contains(method), "method는 MANUAL 또는 AI_ASSISTED여야 합니다.");
        require(trial >= 1 && trial <= 100, "trial은 1~100이어야 합니다.");
        require(Set.of("END_TO_END", "REVIEW_ONLY").contains(scope), "측정 범위는 END_TO_END 또는 REVIEW_ONLY여야 합니다.");
        ObjectNode record = object().put("incidentId", input.path("incidentId").asText())
                .put("sourceType", input.path("sourceType").asText()).put("inputSha256", hash(input))
                .put("method", method).put("trial", trial).put("measurementScope", scope).put("startedAt", Instant.now().toString());
        writeNew(session, record);
    }

    static Path stop(Path session, Path notes) throws IOException {
        return stop(session, notes, Instant.now());
    }

    static Path stop(Path session, Path notes, Instant ended) throws IOException {
        JsonNode saved = read(session);
        fields(saved, Set.of("incidentId", "sourceType", "inputSha256", "method", "trial", "startedAt", "measurementScope"));
        require(Set.of("MANUAL", "AI_ASSISTED").contains(text(saved, "method", 20))
                && Set.of("SIMULATION", "HISTORICAL", "LIVE").contains(text(saved, "sourceType", 20))
                && text(saved, "inputSha256", 64).matches("[a-f0-9]{64}")
                && saved.path("trial").isIntegralNumber() && saved.path("trial").asInt() >= 1
                && saved.path("trial").asInt() <= 100, "측정 세션 형식이 올바르지 않습니다.");
        text(saved, "incidentId", 100);
        ObjectNode record = (ObjectNode) saved;
        String scope = record.path("measurementScope").asText("END_TO_END");
        require(Set.of("END_TO_END", "REVIEW_ONLY").contains(scope), "측정 범위가 올바르지 않습니다.");
        JsonNode assessment = read(notes);
        fields(assessment, Set.of("causeCandidates", "additionalChecks", "diagnosisReport"));
        for (String key : List.of("causeCandidates", "additionalChecks")) {
            require(assessment.path(key).isArray() && !assessment.path(key).isEmpty() && assessment.path(key).size() <= 20,
                    "측정을 끝내려면 causeCandidates와 additionalChecks 배열을 작성하세요.");
            for (JsonNode item : assessment.path(key)) require(item.isTextual() && !item.asText().isBlank()
                    && item.asText().length() <= 2000, "검토 기록은 2000자 이하 문자열이어야 합니다.");
        }
        if (record.path("method").asText().equals("AI_ASSISTED")) {
            Path reportPath = notes.toAbsolutePath().getParent().resolve(text(assessment, "diagnosisReport", 1000));
            JsonNode report = read(reportPath);
            new IncidentAnalyzer(null, null, "gpt-4.1-mini", report.path("promptVersion").asText(IncidentAnalyzer.PROMPT_VERSION))
                    .validateDiagnosis(report.path("diagnosis"), report.path("analyzedInput"));
            require(report.path("inputSha256").asText().equals(record.path("inputSha256").asText())
                    && report.path("mode").asText().equals("multi") && report.path("omittedMetrics").isArray()
                    && report.path("omittedMetrics").isEmpty() && report.path("diagnosis").isObject(),
                    "시간 비교에는 동일 입력의 multi 전체 지표 진단 보고서가 필요합니다.");
            Instant started = IncidentInput.instant(text(record, "startedAt", 40));
            Instant analyzed = IncidentInput.instant(text(report, "analysisStartedAt", 40));
            if (scope.equals("END_TO_END")) require(!analyzed.isBefore(started) && !analyzed.isAfter(ended), "AI 분석을 측정 시작 이후에 실행해야 합니다.");
            else require(!IncidentInput.instant(text(report, "analysisCompletedAt", 40)).isAfter(started),
                    "REVIEW_ONLY는 진단 생성이 완료된 뒤 측정을 시작해야 합니다.");
            record.put("diagnosisReportSha256", hash(report));
        }
        long elapsed = Duration.between(IncidentInput.instant(text(record, "startedAt", 40)), ended).toMillis();
        require(elapsed > 0 && elapsed <= Duration.ofHours(24).toMillis(), "측정 시간은 0초 초과, 24시간 이하여야 합니다.");
        record.put("endedAt", ended.toString()).put("elapsedMs", elapsed).set("review", IncidentInput.redact(assessment));
        Path output = session.resolveSibling(session.getFileName() + ".measurement.json");
        writeNew(output, record);
        return output;
    }

    static ObjectNode summarize(Path directory) throws IOException {
        Map<String, Map<Integer, Map<String, Long>>> groups = new HashMap<>();
        Map<String, JsonNode> labels = new HashMap<>();
        try (var files = Files.list(directory)) {
            for (Path path : files.filter(p -> p.getFileName().toString().endsWith(".measurement.json")).sorted().toList()) {
                JsonNode record = read(path);
                String hash = text(record, "inputSha256", 64);
                String method = text(record, "method", 20);
                require(hash.matches("[a-f0-9]{64}") && Set.of("MANUAL", "AI_ASSISTED").contains(method), "측정 기록 형식이 잘못됐습니다.");
                require(record.path("elapsedMs").isIntegralNumber() && record.path("elapsedMs").asLong() > 0
                        && record.path("elapsedMs").asLong() <= Duration.ofHours(24).toMillis()
                        && record.path("trial").isIntegralNumber() && record.path("trial").asInt() >= 1
                        && record.path("trial").asInt() <= 100, "측정 시간 또는 회차가 잘못됐습니다.");
                String scope = record.path("measurementScope").asText("END_TO_END");
                require(Set.of("END_TO_END", "REVIEW_ONLY").contains(scope), "측정 범위가 올바르지 않습니다.");
                String key = text(record, "incidentId", 100) + ":" + text(record, "sourceType", 20) + ":" + hash + ":" + scope;
                labels.put(key, record);
                var pair = groups.computeIfAbsent(key, unused -> new HashMap<>())
                        .computeIfAbsent(record.path("trial").asInt(), unused -> new HashMap<>());
                require(pair.putIfAbsent(method, record.path("elapsedMs").asLong()) == null, "동일 입력·회차·방법의 측정이 중복됐습니다.");
            }
        }
        ObjectNode summary = object();
        summary.put("primaryStatistic", "MEDIAN");
        summary.put("note", "동일 입력·회차·측정 범위의 수동/AI 보조 쌍을 비교합니다. 중앙값 기준 단축률이며 평균도 별도 제공합니다.");
        var cases = summary.putArray("cases");
        List<Long> allManual = new ArrayList<>(), allAi = new ArrayList<>();
        int unmatched = 0;
        Set<String> scopes = new java.util.HashSet<>();
        for (String key : groups.keySet().stream().sorted().toList()) {
            List<Long> manual = new ArrayList<>(), ai = new ArrayList<>();
            int missing = 0;
            for (var pair : groups.get(key).values()) {
                if (pair.size() != 2) { missing++; continue; }
                manual.add(pair.get("MANUAL")); ai.add(pair.get("AI_ASSISTED"));
            }
            JsonNode label = labels.get(key);
            String scope = label.path("measurementScope").asText("END_TO_END");
            scopes.add(scope);
            ObjectNode row = stats(manual, ai).put("incidentId", label.path("incidentId").asText())
                    .put("sourceType", label.path("sourceType").asText()).put("inputSha256", label.path("inputSha256").asText()).put("measurementScope", scope)
                    .put("unpairedTrials", missing).put("atLeastThreePairs", manual.size() >= 3);
            cases.add(row);
            allManual.addAll(manual); allAi.addAll(ai); unmatched += missing;
        }
        if (scopes.size() <= 1) summary.set("overall", stats(allManual, allAi).put("unpairedTrials", unmatched)
                .put("measurementScope", scopes.stream().findFirst().orElse("END_TO_END")));
        else summary.putNull("overall"); // End-to-end duration and review-only duration must never be pooled.
        return summary;
    }

    private static ObjectNode stats(List<Long> manual, List<Long> ai) {
        ObjectNode row = object().put("pairedTrials", manual.size());
        if (manual.isEmpty()) {
            row.putNull("manualMeanSeconds").putNull("aiAssistedMeanSeconds").putNull("reductionPercent")
                    .putNull("manualMedianSeconds").putNull("aiAssistedMedianSeconds").putNull("meanReductionPercent");
            return row;
        }
        double m = manual.stream().mapToLong(Long::longValue).average().orElseThrow() / 1000;
        double a = ai.stream().mapToLong(Long::longValue).average().orElseThrow() / 1000;
        double mm = median(manual), am = median(ai);
        return row.put("manualMeanSeconds", m).put("aiAssistedMeanSeconds", a).put("meanReductionPercent", (m - a) / m * 100)
                .put("manualMedianSeconds", mm).put("aiAssistedMedianSeconds", am).put("reductionPercent", (mm - am) / mm * 100);
    }

    private static double median(List<Long> values) {
        List<Long> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? sorted.get(middle) / 1000.0 : (sorted.get(middle - 1) / 2.0 + sorted.get(middle) / 2.0) / 1000;
    }
}
