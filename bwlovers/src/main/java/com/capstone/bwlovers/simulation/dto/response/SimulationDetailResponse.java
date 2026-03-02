package com.capstone.bwlovers.simulation.dto.response;

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
        List<ContractResponse> contractResponses = (s.getContracts() == null) ? List.of()
                : s.getContracts().stream().map(ContractResponse::from).toList();

        return SimulationDetailResponse.builder()
                .simulationId(s.getId())
                .resultId(s.getResultId())
                .insuranceCompany(s.getInsuranceCompany())
                .productName(s.getProductName())
                .question(s.getQuestion())
                .result(s.getResult())
                .createdAt(s.getCreatedAt())
                .contracts(contractResponses)
                .build();
    }
}