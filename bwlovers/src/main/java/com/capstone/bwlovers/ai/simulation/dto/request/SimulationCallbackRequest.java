package com.capstone.bwlovers.ai.simulation.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class SimulationCallbackRequest {

    @JsonProperty("simulationId")
    private String simulationId;

    @JsonProperty("expiresInSec")
    private Integer expiresInSec;

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
    @NoArgsConstructor
    public static class SpecialContract {

        @JsonProperty("contract_name")
        private String contractName;

        @JsonProperty("page_number")
        private Integer pageNumber;
    }
}