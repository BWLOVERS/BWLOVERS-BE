package com.capstone.bwlovers.ops.incident;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.capstone.bwlovers.ops.incident.IncidentJson.*;

final class IncidentEvaluation {
    private IncidentEvaluation() { }

    static ObjectNode evaluate(Path reports, Path expectations) throws IOException {
        JsonNode expected = read(expectations);
        require(expected.isArray(), "기대 후보 파일은 배열이어야 합니다.");
        Map<String, JsonNode> cases = new HashMap<>();
        for (JsonNode entry : expected) {
            fields(entry, Set.of("incidentId", "sourceType", "expectedCandidates"));
            String id = text(entry, "incidentId", 100);
            require(Set.of("SIMULATION", "HISTORICAL", "LIVE").contains(text(entry, "sourceType", 20)), "기대 후보의 출처 구분이 잘못됐습니다.");
            require(entry.path("expectedCandidates").isArray() && !entry.path("expectedCandidates").isEmpty(), "기대 후보가 필요합니다.");
            for (JsonNode code : entry.path("expectedCandidates")) require(code.isTextual()
                    && Set.of("MEMORY_PRESSURE", "INSTANCE_CAPACITY", "DB_POOL_EXHAUSTION", "REQUEST_LATENCY", "OTHER", "INSUFFICIENT_DATA").contains(code.asText()),
                    "기대 후보 코드가 올바르지 않습니다.");
            require(cases.putIfAbsent(id, entry) == null, "기대 후보의 incidentId가 중복됐습니다.");
        }
        ObjectNode evaluation = object().put("note", "후보 코드의 포함 여부만 비교합니다. 원인 확정/AI 정확도 측정이 아니며 근거 해석은 사람이 검토해야 합니다.");
        var rows = evaluation.putArray("cases");
        Set<String> seen = new TreeSet<>();
        try (var files = Files.walk(reports, 2)) {
            for (Path path : files.filter(p -> p.getFileName().toString().equals("report.json")).sorted().toList()) {
                JsonNode report = read(path);
                String id = text(report, "incidentId", 100);
                require(cases.containsKey(id), "보고서에 대응하는 기대 후보가 없습니다.");
                JsonNode expectation = cases.get(id);
                require(expectation.path("sourceType").equals(report.path("sourceType")), "보고서와 기대 후보의 출처 구분이 다릅니다.");
                new IncidentAnalyzer(null, null, "gpt-4.1-mini", report.path("promptVersion").asText(IncidentAnalyzer.PROMPT_VERSION))
                        .validateDiagnosis(report.path("diagnosis"), report.path("analyzedInput"));
                Set<String> predicted = new TreeSet<>(), expectedCodes = new TreeSet<>();
                report.path("diagnosis").path("causeCandidates").forEach(c -> predicted.add(c.path("code").asText()));
                expectation.path("expectedCandidates").forEach(c -> expectedCodes.add(c.asText()));
                Set<String> matched = new TreeSet<>(predicted); matched.retainAll(expectedCodes);
                Set<String> missing = new TreeSet<>(expectedCodes); missing.removeAll(predicted);
                Set<String> extra = new TreeSet<>(predicted); extra.removeAll(expectedCodes);
                ObjectNode row = rows.addObject().put("incidentId", id).put("sourceType", report.path("sourceType").asText())
                        .put("mode", report.path("mode").asText()).put("inputSha256", report.path("inputSha256").asText())
                        .put("model", report.path("model").asText()).put("promptVersion", report.path("promptVersion").asText())
                        .put("responseId", report.path("responseId").asText())
                        .put("firstCandidate", report.at("/diagnosis/causeCandidates/0/code").asText())
                        .put("firstCandidateExpected", expectedCodes.contains(report.at("/diagnosis/causeCandidates/0/code").asText()))
                        .put("humanReviewStatus", "PENDING")
                        .put("evidenceValuesVerified", true).put("allExpectedCandidatesPresent", missing.isEmpty());
                row.set("omittedMetrics", report.path("omittedMetrics"));
                row.set("expectedCandidates", MAPPER.valueToTree(expectedCodes));
                row.set("predictedCandidates", MAPPER.valueToTree(predicted));
                row.set("matchedCandidates", MAPPER.valueToTree(matched));
                row.set("missingCandidates", MAPPER.valueToTree(missing));
                row.set("additionalCandidates", MAPPER.valueToTree(extra));
                row.set("confidence", report.path("diagnosis").path("confidence"));
                seen.add(id);
            }
        }
        Set<String> notRun = new TreeSet<>(cases.keySet()); notRun.removeAll(seen);
        evaluation.set("unrunCases", MAPPER.valueToTree(notRun));
        return evaluation;
    }
}
