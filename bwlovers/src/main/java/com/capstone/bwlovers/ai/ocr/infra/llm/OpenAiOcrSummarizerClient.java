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

    private static final long OPENAI_TIMEOUT_SEC = 120L;

    private final WebClient openAiWebClient;

    @Value("${openai.model:gpt-4.1-mini}")
    private String model;

    public OcrResult summarize(String mergedOcrText) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "input", List.of(
                            Map.of("role", "system", "content",
                                    """
                                    너는 보험 OCR 안전 요약기다.
                                    반드시 OCR 텍스트에 직접 근거가 있는 내용만 설명하고, 없는 보장 내용이나 숫자를 추정하면 안 된다.
                                    먼저 문서가 보험 관련인지 분류한 뒤, 보험 문서가 아니거나 OCR 품질이 낮으면 그 사실을 명확히 알리고 안전한 안내만 제공한다.
                                    불확실한 정보는 반드시 '확인 필요'라고 표현한다.
                                    출력은 설명문 없이 스키마에 맞는 JSON만 반환한다.
                                    """
                            ),
                            Map.of("role", "user", "content",
                                    buildUserPrompt(mergedOcrText)
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
                    .timeout(Duration.ofSeconds(OPENAI_TIMEOUT_SEC))
                    .block();

            if (resp == null || resp.output == null || resp.output.isEmpty()) {
                throw new CustomException(ExceptionCode.AI_PROCESSING_FAILED);
            }

            String jsonText = extractJsonText(resp);

            if (jsonText == null || jsonText.isBlank()) {
                throw new CustomException(ExceptionCode.AI_PROCESSING_FAILED);
            }

            return Jsons.read(jsonText, OcrResult.class).normalized();

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
                "required", List.of(
                        "documentCategory",
                        "insuranceDocument",
                        "processingNote",
                        "detectedSignals",
                        "recommendedActions",
                        "oneLineSummary",
                        "easyExplanation",
                        "importantPoints",
                        "warnings",
                        "terms"
                ),
                "properties", Map.of(
                        "documentCategory", Map.of(
                                "type", "string",
                                "enum", List.of("INSURANCE", "NON_INSURANCE", "UNCERTAIN", "INSUFFICIENT_TEXT")
                        ),
                        "insuranceDocument", Map.of("type", "boolean"),
                        "processingNote", Map.of("type", "string"),
                        "detectedSignals", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"),
                                "minItems", 0,
                                "maxItems", 6
                        ),
                        "recommendedActions", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"),
                                "minItems", 0,
                                "maxItems", 6
                        ),
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

    private String buildUserPrompt(String mergedOcrText) {
        return """
                아래 OCR 텍스트를 읽고, 먼저 문서 유형을 분류한 뒤 결과를 JSON으로 만들어줘.

                분류 기준:
                - INSURANCE: 보험 약관, 상품설명서, 보장표, 특약 설명, 보험금 지급/면책/갱신/납입 안내처럼 보험 이해에 직접 쓰이는 문서
                - NON_INSURANCE: 영수증, 신분증, 학교/행정 서류, 일반 계약서, 주문/배송 문서, 의료 문서 등 보험 약관 요약 대상이 아닌 문서
                - INSUFFICIENT_TEXT: OCR이 너무 짧거나 깨져서 문서 성격과 내용을 신뢰성 있게 판단하기 어려운 경우
                - UNCERTAIN: 일부 보험 관련 표현이 보이지만 문서 성격을 확정하기 어려운 경우

                반드시 지킬 규칙:
                1. 보험 문서가 아니면 insuranceDocument=false 로 두고, 보장 내용/보험금/면책 조항을 추정하지 마.
                2. OCR 텍스트가 불완전하면 processingNote, warnings 에 확인 필요를 남겨.
                3. detectedSignals 에는 문서 분류에 영향을 준 단어/표현만 넣어.
                4. recommendedActions 에는 사용자가 다음에 하면 좋은 행동을 넣어.
                5. importantPoints, warnings, detectedSignals, recommendedActions 는 각각 짧은 문장 또는 짧은 구로 작성해.
                6. terms 는 실제 텍스트를 이해하는 데 꼭 필요한 보험 용어만 넣고, 비보험 문서면 비워도 된다.
                7. JSON 외 다른 설명은 출력하지 마.

                OCR 텍스트:
                <<<OCR
                %s
                OCR
                """.formatted(mergedOcrText);
    }

    private String extractJsonText(OpenAiResponse response) {
        if (response.output == null) {
            return null;
        }

        for (OpenAiResponse.Output output : response.output) {
            if (output == null || output.content == null) {
                continue;
            }

            for (OpenAiResponse.Content content : output.content) {
                if (content != null && content.text != null && !content.text.isBlank()) {
                    return content.text;
                }
            }
        }

        return null;
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
