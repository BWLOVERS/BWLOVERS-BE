package com.capstone.bwlovers.ops.incident;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.capstone.bwlovers.ops.incident.IncidentJson.*;

final class IncidentAnalyzer {
    static final String PROMPT_VERSION = "incident-v2";
    private final IncidentHttp http;
    private final String apiKey;
    private final String model;
    private final JsonNode schema;
    private final String instructions;
    private final String promptVersion;

    IncidentAnalyzer(IncidentHttp http, String apiKey, String model) throws IOException {
        this(http, apiKey, model, PROMPT_VERSION);
    }

    IncidentAnalyzer(IncidentHttp http, String apiKey, String model, String promptVersion) throws IOException {
        this.http = http;
        this.apiKey = apiKey;
        require(model != null && model.matches("[A-Za-z0-9._-]{1,100}"), "OPENAI_INCIDENT_MODEL 형식이 올바르지 않습니다.");
        this.model = model;
        require(Set.of("incident-v1", "incident-v2").contains(promptVersion), "지원하지 않는 프롬프트 버전입니다.");
        this.promptVersion = promptVersion;
        schema = parse(resource(promptVersion.equals("incident-v1") ? "diagnosis-schema-v1.json" : "diagnosis-schema.json"));
        instructions = resource(promptVersion.equals("incident-v1") ? "instructions-v1.txt" : "instructions.txt");
    }

    String promptVersion() { return promptVersion; }

    ObjectNode request(JsonNode input) {
        ObjectNode request = object();
        request.put("model", model).put("store", false).put("max_output_tokens", 6000);
        request.put("instructions", instructions);
        request.put("input", input.toString());
        request.putObject("text").putObject("format")
                .put("type", "json_schema").put("name", "incident_diagnosis").put("strict", true).set("schema", schema);
        return request;
    }

    Result analyze(JsonNode input) throws IOException, InterruptedException {
        require(apiKey != null && !apiKey.isBlank(), "OPENAI_API_KEY 환경 변수가 필요합니다. 입력 확인에는 --dry-run을 사용하세요.");
        require(apiKey.matches("[\\x21-\\x7E]+"), "OPENAI_API_KEY에 공백 또는 허용되지 않은 문자가 있습니다.");
        JsonNode response = http.post(URI.create("https://api.openai.com/v1/responses"),
                Map.of("Authorization", "Bearer " + apiKey), request(input));
        require(response.path("status").asText().equals("completed"), "AI 응답이 완료되지 않았습니다. 결과를 진단으로 사용하지 않습니다.");
        String json = null;
        for (JsonNode item : response.path("output")) {
            if (!item.path("type").asText().equals("message")) continue;
            for (JsonNode content : item.path("content")) {
                require(!content.path("type").asText().equals("refusal"), "AI가 분석을 거절했습니다.");
                if (content.path("type").asText().equals("output_text")) {
                    require(json == null, "AI가 여러 진단을 반환했습니다.");
                    json = content.path("text").asText();
                }
            }
        }
        require(json != null, "AI 응답에 진단 JSON이 없습니다.");
        JsonNode diagnosis = parse(json);
        validateDiagnosis(diagnosis, input);
        return new Result(diagnosis, response.path("id").asText(), response.path("model").asText(model), response.path("usage"));
    }

    void validateDiagnosis(JsonNode diagnosis, JsonNode input) {
        validate(diagnosis, schema);
        Set<String> evidenceFields = new HashSet<>();
        for (JsonNode evidence : diagnosis.path("evidence")) {
            String field = evidence.path("field").asText();
            require(field.matches("/relatedLogs/[0-9]+/message") ||
                    IncidentInput.METRICS.stream().anyMatch(metric -> field.equals("/" + metric)), "허용되지 않은 근거 경로입니다.");
            JsonNode actual = input.at(field);
            require(!actual.isMissingNode() && !actual.isNull() && actual.isValueNode()
                    && actual.asText().equals(evidence.path("value").asText()), "AI 근거 값이 실제 입력과 일치하지 않습니다.");
            require(evidenceFields.add(field), "AI 근거 경로가 중복됐습니다.");
        }
        Set<String> codes = new HashSet<>();
        for (JsonNode candidate : diagnosis.path("causeCandidates")) {
            require(codes.add(candidate.path("code").asText()), "AI 원인 후보 코드가 중복됐습니다.");
            for (JsonNode field : candidate.path("evidenceFields")) {
                require(evidenceFields.contains(field.asText()), "원인 후보가 검증되지 않은 근거를 참조합니다.");
            }
        }
        if (IncidentInput.METRICS.stream().noneMatch(input::hasNonNull) && input.path("relatedLogs").size() == 1) {
            require(diagnosis.path("confidence").path("level").asText().equals("LOW"), "단일 로그 진단의 확신도는 LOW여야 합니다.");
        }
    }

    private static String resource(String name) throws IOException {
        try (var stream = IncidentAnalyzer.class.getResourceAsStream("/incident/" + name)) {
            if (stream == null) throw new IOException("진단 리소스를 찾을 수 없습니다.");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    record Result(JsonNode diagnosis, String responseId, String model, JsonNode usage) { }
}
