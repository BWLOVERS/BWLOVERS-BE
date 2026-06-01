package com.capstone.bwlovers.ai.ocr.domain;

import lombok.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrResult {

    @Builder.Default
    private OcrDocumentCategory documentCategory = OcrDocumentCategory.UNCERTAIN;
    private boolean insuranceDocument;
    private String processingNote; // 분류/처리 메모
    @Builder.Default
    private List<String> detectedSignals = List.of(); // 문서 판별 근거
    @Builder.Default
    private List<String> recommendedActions = List.of(); // 사용자 다음 행동 안내
    private String oneLineSummary; // 한 줄 요약
    private String easyExplanation; // 쉽게 풀어쓴 전체 설명
    @Builder.Default
    private List<String> importantPoints = List.of(); // 꼭 알아야 할 핵심 포인트
    @Builder.Default
    private List<String> warnings = List.of(); // 주의해야 할 부분
    @Builder.Default
    private List<TermDefinition> terms = List.of(); // 어려운 용어 풀이

    public OcrResult normalized() {
        OcrDocumentCategory normalizedCategory =
                (documentCategory == null) ? OcrDocumentCategory.UNCERTAIN : documentCategory;

        return OcrResult.builder()
                .documentCategory(normalizedCategory)
                .insuranceDocument(insuranceDocument)
                .processingNote(defaultString(processingNote, defaultProcessingNote(normalizedCategory)))
                .detectedSignals(safeStrings(detectedSignals))
                .recommendedActions(safeStrings(recommendedActions))
                .oneLineSummary(defaultString(oneLineSummary, defaultSummary(normalizedCategory)))
                .easyExplanation(defaultString(easyExplanation, defaultExplanation(normalizedCategory)))
                .importantPoints(safeStrings(importantPoints))
                .warnings(safeStrings(warnings))
                .terms(safeTerms(terms))
                .build();
    }

    public static OcrResult insufficientText(List<String> detectedSignals) {
        return OcrResult.builder()
                .documentCategory(OcrDocumentCategory.INSUFFICIENT_TEXT)
                .insuranceDocument(false)
                .processingNote("OCR 텍스트가 너무 적거나 깨져 있어서 보험 문서 여부를 안정적으로 판단하지 않았습니다.")
                .detectedSignals(safeStrings(detectedSignals))
                .recommendedActions(List.of(
                        "글자가 선명하게 보이도록 다시 촬영해 주세요.",
                        "보험 약관, 보장표, 특약 페이지처럼 본문이 많은 페이지를 포함해 주세요."
                ))
                .oneLineSummary("OCR 텍스트가 부족해 보험 문서 요약을 만들지 않았습니다.")
                .easyExplanation("현재 업로드본에서는 문서 핵심 내용이 충분히 읽히지 않아 보장 내용이나 주의사항을 안전하게 정리할 수 없습니다.")
                .importantPoints(List.of("텍스트 분량 또는 인식 품질이 부족했습니다."))
                .warnings(List.of("불충분한 OCR 결과에서 보장 내용을 추정하면 잘못된 안내가 될 수 있어 요약을 제한했습니다."))
                .terms(List.of())
                .build();
    }

    public static OcrResult nonInsurance(List<String> detectedSignals) {
        return OcrResult.builder()
                .documentCategory(OcrDocumentCategory.NON_INSURANCE)
                .insuranceDocument(false)
                .processingNote("보험 약관/상품설명서/보장표로 보기 어려운 문서라서 보험 요약을 생성하지 않았습니다.")
                .detectedSignals(safeStrings(detectedSignals))
                .recommendedActions(List.of(
                        "보험 약관, 상품설명서, 보장내용표 이미지를 업로드해 주세요.",
                        "영수증, 신분증, 학교/행정 문서는 이 OCR 요약 대상이 아닙니다."
                ))
                .oneLineSummary("보험 관련 문서로 판단되지 않아 요약을 제한했습니다.")
                .easyExplanation("업로드된 문서에서 보험금, 보장, 면책, 특약 같은 보험 핵심 신호보다 다른 문서 신호가 더 강하게 보여 안전한 보험 요약을 제공하지 않았습니다.")
                .importantPoints(List.of("문서 성격이 보험 안내/약관 문서와 다르게 보입니다."))
                .warnings(List.of("보험과 무관한 문서에 대해 보장내용을 추정하면 잘못된 설명이 될 수 있습니다."))
                .terms(List.of())
                .build();
    }

    private static String defaultString(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private static String defaultProcessingNote(OcrDocumentCategory category) {
        return switch (category) {
            case INSURANCE -> "OCR 텍스트 기준으로 보험 관련 문서로 판단해 요약했습니다.";
            case NON_INSURANCE -> "보험 관련 문서로 보기 어려워 요약을 제한했습니다.";
            case INSUFFICIENT_TEXT -> "OCR 텍스트가 부족해 문서 분류와 요약을 제한했습니다.";
            case UNCERTAIN -> "보험 문서 여부가 불확실해 제한적으로 정리했습니다.";
        };
    }

    private static String defaultSummary(OcrDocumentCategory category) {
        return switch (category) {
            case INSURANCE -> "보험 문서를 OCR 기준으로 요약했습니다.";
            case NON_INSURANCE -> "보험 관련 문서가 아닌 것으로 보여 요약을 제한했습니다.";
            case INSUFFICIENT_TEXT -> "OCR 텍스트가 부족해 요약을 만들지 않았습니다.";
            case UNCERTAIN -> "문서 성격이 불확실해 제한적으로 정리했습니다.";
        };
    }

    private static String defaultExplanation(OcrDocumentCategory category) {
        return switch (category) {
            case INSURANCE -> "OCR에 잡힌 보험 문서 내용을 바탕으로 이해하기 쉬운 형태로 정리했습니다.";
            case NON_INSURANCE -> "보험 문서가 아닐 가능성이 높아, 없는 보장 내용이나 약관 조항을 추정하지 않았습니다.";
            case INSUFFICIENT_TEXT -> "문서 글자가 너무 적거나 깨져 있어서 신뢰할 수 있는 설명을 제공하지 않았습니다.";
            case UNCERTAIN -> "문서 일부는 읽히지만 보험 문서 여부를 단정하기 어려워 확인이 필요한 수준으로만 정리했습니다.";
        };
    }

    private static List<String> safeStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        List<String> sanitized = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                sanitized.add(value.trim());
            }
        }
        return sanitized.isEmpty() ? List.of() : Collections.unmodifiableList(sanitized);
    }

    private static List<TermDefinition> safeTerms(List<TermDefinition> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        List<TermDefinition> sanitized = new ArrayList<>();
        for (TermDefinition value : values) {
            if (value == null) {
                continue;
            }

            String term = (value.getTerm() == null) ? "" : value.getTerm().trim();
            String meaning = (value.getMeaning() == null) ? "" : value.getMeaning().trim();
            if (term.isBlank() || meaning.isBlank()) {
                continue;
            }

            sanitized.add(TermDefinition.builder()
                    .term(term)
                    .meaning(meaning)
                    .build());
        }
        return sanitized.isEmpty() ? List.of() : Collections.unmodifiableList(sanitized);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TermDefinition {
        private String term;
        private String meaning;
    }
}
