package com.capstone.bwlovers.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class InsuranceRecommendationResponse {

    @JsonProperty("insurance_company")
    private String insuranceCompany;

    @JsonProperty("is_long_term")
    private boolean isLongTerm;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("insurance_recommendation_reason")
    private String insuranceRecommendationReason;

    @JsonProperty("monthly_cost")
    private Integer monthlyCost;

    @JsonProperty("special_contracts")
    private List<SpecialContract> specialContracts;

    @JsonProperty("evidence_sources")
    private List<EvidenceSource> evidenceSources;

    @Getter
    @Setter
    public static class SpecialContract {
        @JsonProperty("contract_name")
        private String contractName;

        @JsonProperty("contract_description")
        private String contractDescription;

        @JsonProperty("contract_recommendation_reason")
        private String contractRecommendationReason;

        @JsonProperty("key_features")
        private List<String> keyFeatures;

        @JsonProperty("page_number")
        private Integer pageNumber;
    }

    @Getter
    @Setter
    public static class EvidenceSource {
        @JsonProperty("page_number")
        private Integer pageNumber;

        @JsonProperty("text_snippet")
        private String textSnippet;
    }
}
