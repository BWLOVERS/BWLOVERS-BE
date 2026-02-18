package com.capstone.bwlovers.ai.analysis.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisRequest {

    @JsonProperty("insurance_company")
    private String insuranceCompany;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("special_contracts")
    private List<SpecialContract> specialContracts;

    @JsonProperty("question")
    private String question;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpecialContract {

        @JsonProperty("contract_name")
        private String contractName;

        @JsonProperty("page_number")
        private Integer pageNumber;
    }
}