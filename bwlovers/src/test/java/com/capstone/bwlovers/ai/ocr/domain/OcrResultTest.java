package com.capstone.bwlovers.ai.ocr.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcrResultTest {

    @Test
    void normalizedFillsDefaultsAndRemovesBlankEntries() {
        OcrResult result = OcrResult.builder()
                .documentCategory(OcrDocumentCategory.UNCERTAIN)
                .insuranceDocument(false)
                .processingNote(" ")
                .detectedSignals(List.of("  보험  ", "", "  "))
                .recommendedActions(null)
                .oneLineSummary(null)
                .easyExplanation(" ")
                .importantPoints(List.of("핵심", " "))
                .warnings(null)
                .terms(List.of(
                        OcrResult.TermDefinition.builder().term(" 면책 ").meaning(" 보장하지 않는 경우 ").build(),
                        OcrResult.TermDefinition.builder().term(" ").meaning(" ").build()
                ))
                .build()
                .normalized();

        assertEquals(OcrDocumentCategory.UNCERTAIN, result.getDocumentCategory());
        assertFalse(result.isInsuranceDocument());
        assertTrue(result.getProcessingNote().contains("불확실"));
        assertEquals(List.of("보험"), result.getDetectedSignals());
        assertTrue(result.getRecommendedActions().isEmpty());
        assertTrue(result.getOneLineSummary().contains("불확실"));
        assertTrue(result.getEasyExplanation().contains("확인"));
        assertEquals(List.of("핵심"), result.getImportantPoints());
        assertTrue(result.getWarnings().isEmpty());
        assertEquals(1, result.getTerms().size());
        assertEquals("면책", result.getTerms().get(0).getTerm());
    }
}
