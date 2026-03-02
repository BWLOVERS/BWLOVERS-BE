package com.capstone.bwlovers.insurance.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class InsuranceDetailListResponse {
    private Long insuranceId;
    private String insuranceCompany;
    private String productName;
    private boolean isLongTerm;
    private Long sumInsured;
    private String monthlyCost;
    private String memo;
    private LocalDateTime createdAt;
    private List<SpecialContractResponse> specialContracts;

    @Getter
    @Builder
    public static class SpecialContractResponse {
        private Long contractId;
        private String contractName;
    }
}