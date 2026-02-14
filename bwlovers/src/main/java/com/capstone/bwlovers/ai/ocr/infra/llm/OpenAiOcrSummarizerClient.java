package com.capstone.bwlovers.ai.ocr.infra.llm;

import com.capstone.bwlovers.ai.ocr.domain.OcrResult;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.capstone.bwlovers.global.util.Jsons;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiOcrSummarizerClient {

    private final WebClient openAiWebClient;

    @Value("${openai.model:gpt-4.1-mini}")
    private String model;

    public OcrResult summarize(String mergedOcrText) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "input", List.of(
                            Map.of("role", "system", "content",
                                    "너는 '보험 이해' 도우미임. 과장하지 말고, 약관에 적힌 내용만 근거로 쉽게 설명해야 함. "
                                            + "정보가 불충분하면 '확인 필요함'이라고 써야 함. 출력은 반드시 스키마에 맞는 JSON만 반환해야 함."
                            ),
                            Map.of("role", "user", "content",
                                    "아래는 보험 약관 OCR 텍스트임. 사용자가 이해하기 쉽게 요약/주의/용어풀이를 만들어줘.\n\n"
                                            + mergedOcrText
                            )
                    ),
                    "text", Map.of(
                            "format", Map.of(
                                    "type", "json_schema",
                                    "name", "OcrResult",
                                    "schema", ocrResultSchema()
                            )
                    ),
                    "temperature", 0.2
            );

            OpenAiResponse resp = openAiWebClient.post()
                    .uri("/responses")
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, r ->
                            r.bodyToMono(String.class).map(errBody -> {
                                log.error("[OpenAI] HTTP {} errorBody={}", r.statusCode().value(), errBody);
                                // 여기서 예외를 던져야 bodyToMono로 안 내려감
                                return new RuntimeException("OpenAI HTTP " + r.statusCode().value());
                            })
                    )
                    .bodyToMono(OpenAiResponse.class)
                    .timeout(Duration.ofSeconds(25))
                    .block();

            if (resp == null || resp.output == null || resp.output.isEmpty()) {
                throw new CustomException(ExceptionCode.AI_PROCESSING_FAILED);
            }

            OpenAiResponse.Output first = resp.output.get(0);

            if (first.content == null || first.content.isEmpty()) {
                throw new CustomException(ExceptionCode.AI_PROCESSING_FAILED);
            }

            String jsonText = first.content.get(0).text;

            if (jsonText == null || jsonText.isBlank()) {
                throw new CustomException(ExceptionCode.AI_PROCESSING_FAILED);
            }

            return Jsons.read(jsonText, OcrResult.class);

        } catch (WebClientResponseException e) {
            log.error("[OpenAI] WebClientResponseException status={} body={}",
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString(),
                    e);
            throw new CustomException(ExceptionCode.AI_SERVER_ERROR);

        } catch (CustomException e) {
            throw e;

        } catch (Exception e) {
            log.error("[OpenAI] call failed", e);
            throw new CustomException(ExceptionCode.AI_SERVER_ERROR);
        }
    }

    private Map<String, Object> ocrResultSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("oneLineSummary", "easyExplanation", "importantPoints", "warnings", "terms"),
                "properties", Map.of(
                        "oneLineSummary", Map.of("type", "string"),
                        "easyExplanation", Map.of("type", "string"),
                        "importantPoints", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"),
                                "minItems", 0,
                                "maxItems", 6
                        ),
                        "warnings", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"),
                                "minItems", 0,
                                "maxItems", 6
                        ),
                        "terms", Map.of(
                                "type", "array",
                                "items", Map.of(
                                        "type", "object",
                                        "additionalProperties", false,
                                        "required", List.of("term", "meaning"),
                                        "properties", Map.of(
                                                "term", Map.of("type", "string"),
                                                "meaning", Map.of("type", "string")
                                        )
                                ),
                                "minItems", 0,
                                "maxItems", 8
                        )
                )
        );
    }

    public static class OpenAiResponse {

        public List<Output> output;

        public static class Output {
            public List<Content> content;
        }

        public static class Content {
            public String type;
            public String text;
        }
    }
}