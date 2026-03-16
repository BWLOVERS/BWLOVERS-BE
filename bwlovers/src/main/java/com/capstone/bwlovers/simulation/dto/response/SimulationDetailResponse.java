package com.capstone.bwlovers.simulation.dto.response;

import com.capstone.bwlovers.ai.analysis.dto.response.AnalysisResultResponse;
import com.capstone.bwlovers.simulation.domain.Simulation;
import com.capstone.bwlovers.simulation.domain.SimulationContract;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SimulationDetailResponse {

    private Long simulationId;
    private String resultId;

    private String insuranceCompany;
    private String productName;
    private Boolean longTerm;
    private Long sumInsured;
    private String monthlyCost;
    private String memo;

    private String question;
    private String result;

    private LocalDateTime createdAt;

    private List<ContractResponse> contracts;

    @Getter
    @Builder
    public static class ContractResponse {
        private String contractName;
        private Long pageNumber;

        public static ContractResponse from(SimulationContract c) {
            return ContractResponse.builder()
                    .contractName(c.getContractName())
                    .pageNumber(c.getPageNumber())
                    .build();
        }
    }

    public static SimulationDetailResponse from(Simulation s) {
        return from(s, null);
    }

    public static SimulationDetailResponse from(Simulation s, AnalysisResultResponse cached) {
        List<ContractResponse> contractResponses = getContractResponses(s, cached);

        return SimulationDetailResponse.builder()
                .simulationId(s.getId())
                .resultId(s.getResultId())
                .insuranceCompany(prefer(cached == null ? null : cached.getInsuranceCompany(), s.getInsuranceCompany()))
                .productName(prefer(cached == null ? null : cached.getProductName(), s.getProductName()))
                .longTerm(cached == null ? null : cached.getLongTerm())
                .sumInsured(cached == null ? null : cached.getSumInsured())
                .monthlyCost(cached == null ? null : cached.getMonthlyCost())
                .memo(cached == null ? null : cached.getMemo())
                .question(prefer(cached == null ? null : cached.getQuestion(), s.getQuestion()))
                .result(prefer(cached == null ? null : cached.getResult(), s.getResult()))
                .createdAt(s.getCreatedAt())
                .contracts(contractResponses)
                .build();
    }

    private static List<ContractResponse> getContractResponses(Simulation s, AnalysisResultResponse cached) {
        if (cached != null && cached.getSpecialContracts() != null) {
            return cached.getSpecialContracts().stream()
                    .map(contract -> ContractResponse.builder()
                            .contractName(contract.getContractName())
                            .pageNumber(contract.getPageNumber() == null ? null : contract.getPageNumber().longValue())
                            .build())
                    .toList();
        }

        return (s.getContracts() == null) ? List.of()
                : s.getContracts().stream().map(ContractResponse::from).toList();
    }

    private static String prefer(String cachedValue, String fallbackValue) {
        if (cachedValue == null || cachedValue.isBlank()) {
            return fallbackValue;
        }
        return cachedValue;
    }
}
