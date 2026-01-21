package com.capstone.bwlovers.health.dto.request;

import com.capstone.bwlovers.health.domain.HealthStatus;
import com.capstone.bwlovers.health.domain.healthType.ChronicDiseaseType;
import com.capstone.bwlovers.health.domain.healthType.PregnancyComplicationType;
import com.capstone.bwlovers.health.domain.healthType.PastDiseaseType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthStatusRequest {

    private List<PastDiseaseItem> pastDiseases;
    private List<ChronicDiseaseItem> chronicDiseases;
    private List<PregnancyComplicationItem> pregnancyComplications;

    @Builder
    @Getter
    public static class PregnancyComplicationItem {
        private PregnancyComplicationType pregnancyComplicationType;
    }

    public static HealthStatusRequest from(HealthStatus healthStatus) {
        return HealthStatusRequest.builder()
                .pastDiseases(
                        healthStatus.getPastDiseases().stream()
                                .map(p -> PastDiseaseItem.builder()
                                        .pastDiseaseType(p.getPastDiseaseType())
                                        .pastCured(p.isPastCured())
                                        .pastLastTreatedAt(String.valueOf(p.getPastLastTreatedAt()))
                                        .build()
                                ).toList()
                )
                .chronicDiseases(
                        healthStatus.getChronicDiseases().stream()
                                .map(c -> ChronicDiseaseItem.builder()
                                        .chronicDiseaseType(c.getChronicDiseaseType())
                                        .chronicOnMedication(c.isChronicOnMedication())
                                        .build()
                                ).toList()
                )
                .pregnancyComplications(
                        healthStatus.getPregnancyComplications().stream()
                                .map(pc -> PregnancyComplicationItem.builder()
                                        .pregnancyComplicationType(pc.getPregnancyComplicationType())
                                        .build()
                                ).toList()
                )
                .build();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PastDiseaseItem {

        @NotNull
        private PastDiseaseType pastDiseaseType;

        @NotNull
        private Boolean pastCured;

        // 월 단위로 받기
        @NotNull
        @Pattern(regexp = "^(19|20)\\d{2}-(0[1-9]|1[0-2])$", message = "pastLastTreatedYm은 YYYY-MM 형식이어야 함")
        private String pastLastTreatedAt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChronicDiseaseItem {
        @NotNull
        private ChronicDiseaseType chronicDiseaseType;

        @NotNull
        private Boolean chronicOnMedication;
    }
}
