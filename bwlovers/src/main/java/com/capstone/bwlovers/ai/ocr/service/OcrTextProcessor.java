package com.capstone.bwlovers.ai.ocr.service;

import com.capstone.bwlovers.ai.ocr.domain.OcrResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class OcrTextProcessor {

    private static final int MIN_TEXT_LENGTH = 80;
    private static final int MIN_INFORMATIVE_CHAR_COUNT = 30;

    private static final List<String> INSURANCE_KEYWORDS = List.of(
            "보험", "약관", "보장", "특약", "보험금", "계약자", "피보험자", "면책", "청약", "해지", "납입", "갱신", "만기"
    );

    private static final List<String> NON_INSURANCE_KEYWORDS = List.of(
            "영수증", "거래명세", "주문번호", "배송", "재학증명", "성적증명", "졸업증명", "주민등록", "운전면허", "여권",
            "사업자등록", "통장사본", "세금계산서", "처방전", "진단서", "진료비", "검사결과"
    );

    public String normalizeAndMerge(List<String> pageTexts) {
        String merged = String.join("\n\n---PAGE---\n\n", pageTexts);
        merged = merged.replaceAll("[ \\t]+", " ");
        merged = merged.replaceAll("\\n{3,}", "\n\n");
        return merged.trim();
    }

    public OcrResult buildGuardResultIfNeeded(String mergedText) {
        if (mergedText == null || mergedText.isBlank()) {
            return OcrResult.insufficientText(List.of("OCR 텍스트를 추출하지 못했습니다."));
        }

        String compact = mergedText.replaceAll("\\s+", " ").trim();
        int informativeCharCount = countInformativeChars(compact);
        List<String> insuranceSignals = findMatchedKeywords(compact, INSURANCE_KEYWORDS);
        List<String> nonInsuranceSignals = findMatchedKeywords(compact, NON_INSURANCE_KEYWORDS);

        if (insuranceSignals.isEmpty() && nonInsuranceSignals.size() >= 2) {
            List<String> detectedSignals = new ArrayList<>();
            for (String signal : nonInsuranceSignals) {
                detectedSignals.add("비보험 신호: " + signal);
            }
            return OcrResult.nonInsurance(detectedSignals);
        }

        boolean tooShort = compact.length() < MIN_TEXT_LENGTH || informativeCharCount < MIN_INFORMATIVE_CHAR_COUNT;
        if (tooShort && insuranceSignals.size() <= 1 && nonInsuranceSignals.size() <= 1) {
            return OcrResult.insufficientText(List.of(
                    "추출된 본문 길이: " + compact.length() + "자",
                    "의미 있는 문자 수: " + informativeCharCount + "자"
            ));
        }

        return null;
    }

    private int countInformativeChars(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch) || isHangul(ch)) {
                count++;
            }
        }
        return count;
    }

    private boolean isHangul(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.HANGUL_SYLLABLES
                || block == Character.UnicodeBlock.HANGUL_JAMO
                || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO;
    }

    private List<String> findMatchedKeywords(String text, List<String> keywords) {
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> matched = new ArrayList<>();
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                matched.add(keyword);
            }
            if (matched.size() >= 5) {
                break;
            }
        }
        return matched;
    }
}
