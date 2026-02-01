package com.capstone.bwlovers.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor
public class AiCallbackRequest {

    @JsonProperty("resultId")
    private String resultId;

    @JsonProperty("expiresInSec")
    private Integer expiresInSec;

    @JsonProperty("items")
    private List<Item> items;

    public List<Item> getItemsOrEmpty() {
        return items == null ? Collections.emptyList() : items;
    }

    @Getter
    @NoArgsConstructor
    public static class Item {

        @JsonProperty("itemId")
        private String itemId;

        @JsonProperty("insurance_company")
        private String insuranceCompany;

        @JsonProperty("product_name")
        private String productName;

        @JsonProperty("is_long_term")
        private Boolean isLongTerm;

        @JsonProperty("monthly_cost")
        private Integer monthlyCost;

        @JsonProperty("insurance_recommendation_reason")
        private String insuranceRecommendationReason;

        @JsonProperty("special_contracts")
        private List<SpecialContract> specialContracts;

        @JsonProperty("evidence_sources")
        private List<EvidenceSource> evidenceSources;

        public boolean isLongTerm() {
            return Boolean.TRUE.equals(isLongTerm);
        }

    }

    @Getter
    @NoArgsConstructor
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
    @NoArgsConstructor
    public static class EvidenceSource {

        @JsonProperty("page_number")
        private Integer pageNumber;

        @JsonProperty("text_snippet")
        private String textSnippet;
    }
}
