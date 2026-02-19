package com.capstone.bwlovers.ai.analysis.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnalysisCreateResponse {

    private String resultId;

    public static AnalysisCreateResponse of(String resultId) {
        return AnalysisCreateResponse.builder()
                .resultId(resultId)
                .build();
    }
}