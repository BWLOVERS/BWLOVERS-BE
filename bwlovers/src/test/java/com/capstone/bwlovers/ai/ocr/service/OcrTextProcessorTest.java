package com.capstone.bwlovers.ai.ocr.service;

import com.capstone.bwlovers.ai.ocr.domain.OcrDocumentCategory;
import com.capstone.bwlovers.ai.ocr.domain.OcrResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OcrTextProcessorTest {

    private final OcrTextProcessor processor = new OcrTextProcessor();

    @Test
    void returnsInsufficientResultWhenTextIsTooShort() {
        OcrResult result = processor.buildGuardResultIfNeeded("보험");

        assertNotNull(result);
        assertEquals(OcrDocumentCategory.INSUFFICIENT_TEXT, result.getDocumentCategory());
        assertFalse(result.isInsuranceDocument());
        assertTrue(result.getOneLineSummary().contains("부족"));
    }

    @Test
    void returnsNonInsuranceResultForObviousNonInsuranceDocument() {
        String text = """
                카드 영수증
                주문번호 12345
                배송 완료
                진료비 영수증
                """;

        OcrResult result = processor.buildGuardResultIfNeeded(text);

        assertNotNull(result);
        assertEquals(OcrDocumentCategory.NON_INSURANCE, result.getDocumentCategory());
        assertFalse(result.isInsuranceDocument());
        assertTrue(result.getDetectedSignals().stream().anyMatch(signal -> signal.contains("영수증")));
    }

    @Test
    void allowsInsuranceLikeTextToReachLlmSummarizer() {
        String text = """
                보험 약관
                계약자와 피보험자의 보장 내용 및 특약, 보험금 지급 사유와 면책 조항을 안내합니다.
                납입 기간과 갱신 조건도 함께 확인해 주세요.
                """;

        OcrResult result = processor.buildGuardResultIfNeeded(text);

        assertNull(result);
    }

    @Test
    void normalizesMergedText() {
        String merged = processor.normalizeAndMerge(List.of(
                "  보험   약관 \n\n\n보장   내용  ",
                "특약\t안내"
        ));

        assertTrue(merged.contains("---PAGE---"));
        assertFalse(merged.contains("   "));
        assertFalse(merged.contains("\n\n\n"));
    }
}
