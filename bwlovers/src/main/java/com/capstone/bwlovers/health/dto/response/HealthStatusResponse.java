package com.capstone.bwlovers.health.dto.response;

import com.capstone.bwlovers.global.entity.BaseTimeEntity;
import com.capstone.bwlovers.health.domain.*;
import com.capstone.bwlovers.health.domain.healthType.ChronicDiseaseType;
import com.capstone.bwlovers.health.domain.healthType.PregnancyComplicationType;
import com.capstone.bwlovers.health.domain.healthType.PastDiseaseType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthStatusResponse {

    private Long statusId;
    private Long userId;
    private LocalDateTime createdAt;

    private List<PastDiseaseResponse> pastDiseases;
    private List<ChronicDiseaseResponse> chronicDiseases;
    private List<PregnancyComplicationResponse> pregnancyComplications;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PastDiseaseResponse {
        private Long pastId;
        private PastDiseaseType pastDiseaseType;
        private boolean pastCured;

        // DB는 DATE(YYYY-MM-01)로 저장하지만, 응답은 month까지만 주는게 깔끔함
        private String pastLastTreatedYm; // "YYYY-MM"
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChronicDiseaseResponse {
        private Long chronicId;
        private ChronicDiseaseType chronicDiseaseType;
        private boolean chronicOnMedication;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PregnancyComplicationResponse {
        private Long complicationId;
        private PregnancyComplicationType pregnancyComplicationType;
    }

    public static HealthStatusResponse from(HealthStatus status) {
        return HealthStatusResponse.builder()
                .statusId(status.getStatusId())
                .userId(status.getUser().getUserId())
                .createdAt(status.getCreatedAt())
                .pastDiseases(status.getPastDiseases().stream().map(HealthStatusResponse::toPast).toList())
                .chronicDiseases(status.getChronicDiseases().stream().map(HealthStatusResponse::toChronic).toList())
                .pregnancyComplications(status.getPregnancyComplications().stream().map(HealthStatusResponse::toComp).toList())
                .build();
    }

    private static PastDiseaseResponse toPast(PastDisease pd) {
        return PastDiseaseResponse.builder()
                .pastId(pd.getPastId())
                .pastDiseaseType(pd.getPastDiseaseType())
                .pastCured(pd.isPastCured())
                .pastLastTreatedYm(toYearMonthString(pd.getPastLastTreatedAt()))
                .build();
    }

    private static ChronicDiseaseResponse toChronic(ChronicDisease cd) {
        return ChronicDiseaseResponse.builder()
                .chronicId(cd.getChronicId())
                .chronicDiseaseType(cd.getChronicDiseaseType())
                .chronicOnMedication(cd.isChronicOnMedication())
                .build();
    }

    private static PregnancyComplicationResponse toComp(PregnancyComplication pc) {
        return PregnancyComplicationResponse.builder()
                .complicationId(pc.getComplicationId())
                .pregnancyComplicationType(pc.getPregnancyComplicationType())
                .build();
    }

    private static String toYearMonthString(LocalDate date) {
        if (date == null) return null;
        // 저장 규칙: 항상 1일로 저장 -> 응답은 "YYYY-MM"만 내려줌
        return String.format("%04d-%02d", date.getYear(), date.getMonthValue());
    }
}
