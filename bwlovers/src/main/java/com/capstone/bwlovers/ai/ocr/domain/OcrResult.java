package com.capstone.bwlovers.ai.ocr.domain;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrResult {

    private String oneLineSummary; // 한 줄 요약
    private String easyExplanation; // 쉽게 풀어쓴 전체 설명
    private List<String> importantPoints; // 꼭 알아야 할 핵심 포인트
    private List<String> warnings; // 주의해야 할 부분
    private List<TermDefinition> terms; // 어려운 용어 풀이

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