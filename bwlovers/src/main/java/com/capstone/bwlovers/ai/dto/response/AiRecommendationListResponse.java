package com.capstone.bwlovers.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AiRecommendationListResponse {

    @JsonProperty("resultId")
    private String resultId;

    @JsonProperty("expiresInSec")
    private Integer expiresInSec;

    @JsonProperty("items")
    private List<Item> items;

    @Getter
    @Setter
    public static class Item {
        @JsonProperty("itemId")
        private String itemId;

        @JsonProperty("insurance_company")
        private String insuranceCompany;

        @JsonProperty("product_name")
        private String productName;

        @JsonProperty("is_long_term")
        private boolean isLongTerm;

        @JsonProperty("monthly_cost")
        private Integer monthlyCost;
    }
}
