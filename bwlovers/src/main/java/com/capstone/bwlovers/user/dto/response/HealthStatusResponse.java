package com.capstone.bwlovers.user.dto.response;

import com.capstone.bwlovers.user.domain.HealthStatus;
import com.capstone.bwlovers.user.domain.healthType.ChronicDiseaseType;
import com.capstone.bwlovers.user.domain.healthType.PastDiseaseType;
import com.capstone.bwlovers.user.domain.healthType.PregnancyComplicationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
public class HealthStatusResponse {

    private Long healthStatusId;

    private Boolean pastCured;
    private LocalDate pastLastTreatedAt;
    private Set<PastDiseaseType> pastDiseases;

    private Set<ChronicDiseaseType> chronicDiseases;

    private Set<PregnancyComplicationType> pregnancyComplications;

    private LocalDateTime updatedAt;

    public static HealthStatusResponse from(HealthStatus hs) {
        return HealthStatusResponse.builder()
                .healthStatusId(hs.getId())
                .pastCured(hs.getPastCured())
                .pastLastTreatedAt(hs.getPastLastTreatedAt())
                .pastDiseases(hs.getPastDiseases())
                .chronicDiseases(hs.getChronicDiseases())
                .pregnancyComplications(hs.getPregnancyComplications())
                .updatedAt(hs.getUpdatedAt())
                .build();
    }
}

