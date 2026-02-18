package com.capstone.bwlovers.ai.simulation.dto.response;

import com.capstone.bwlovers.ai.simulation.dto.request.SimulationCallbackRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SimulationResultResponse {

    @JsonProperty("simulationId")
    private String simulationId;

    @JsonProperty("insurance_company")
    private String insuranceCompany;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("special_contracts")
    private List<SpecialContract> specialContracts;

    @JsonProperty("question")
    private String question;

    @JsonProperty("result")
    private String result;

    @Getter
    @Builder
    public static class SpecialContract {
        @JsonProperty("contract_name")
        private String contractName;

        @JsonProperty("page_number")
        private Integer pageNumber;

        public static SpecialContract of(String name, Integer page) {
            return SpecialContract.builder()
                    .contractName(name)
                    .pageNumber(page)
                    .build();
        }
    }

    public static SimulationResultResponse fromCallback(SimulationCallbackRequest cb) {
        List<SpecialContract> mapped = null;
        if (cb.getSpecialContracts() != null) {
            mapped = cb.getSpecialContracts().stream()
                    .map(sc -> SpecialContract.of(sc.getContractName(), sc.getPageNumber()))
                    .toList();
        }

        return SimulationResultResponse.builder()
                .simulationId(cb.getSimulationId())
                .insuranceCompany(cb.getInsuranceCompany())
                .productName(cb.getProductName())
                .specialContracts(mapped)
                .question(cb.getQuestion())
                .result(cb.getResult())
                .build();
    }
}