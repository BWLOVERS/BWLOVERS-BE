package com.capstone.bwlovers.user.dto.request;

import com.capstone.bwlovers.user.domain.healthType.ChronicDiseaseType;
import com.capstone.bwlovers.user.domain.healthType.PastDiseaseType;
import com.capstone.bwlovers.user.domain.healthType.PregnancyComplicationType;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Set;

@Getter
public class HealthStatusRequest {

    // 과거 병력
    private Boolean pastCured;
    private LocalDate pastLastTreatedAt;
    private Set<PastDiseaseType> pastDiseases;

    // 만성 질환
    private Set<ChronicDiseaseType> chronicDiseases;

    // 이번 임신 확정 진단
    private Set<PregnancyComplicationType> pregnancyComplications;
}
