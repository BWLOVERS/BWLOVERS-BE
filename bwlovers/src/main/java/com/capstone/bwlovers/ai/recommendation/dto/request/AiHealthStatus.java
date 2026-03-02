package com.capstone.bwlovers.ai.recommendation.dto.request;

import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiHealthStatus {

    private List<AiPastDisease> pastDiseases;
    private List<AiChronicDisease> chronicDiseases;

    private List<AiPregnancyComplication> pregnancyComplications;

    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AiPastDisease {
        private String pastDiseaseType;
        private Boolean pastCured;
        private String pastLastTreatedYm;
    }

    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AiChronicDisease {
        private String chronicDiseaseType;
        private Boolean chronicOnMedication;
    }

    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AiPregnancyComplication {
        private Long complicationId;
        private String pregnancyComplicationType;
    }
}