package com.capstone.bwlovers.insurance.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class InsuranceDetailResponse {
    private Long insuranceId;
    private String resultId;
    private String itemId;
    private String insuranceCompany;
    private String productName;
    private boolean isLongTerm;
    private Long sumInsured;
    private String monthlyCost;
    private String insuranceRecommendationReason;
    private String memo;
    private List<SpecialContractDetailDto> specialContracts;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class SpecialContractDetailDto {
        private Long contractId;
        private String contractName;
        private String contractDescription;
        private String contractRecommendationReason;
        private List<String> keyFeatures;
        private Long pageNumber;
    }
}