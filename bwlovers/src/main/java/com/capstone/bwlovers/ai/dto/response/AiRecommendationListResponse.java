package com.capstone.bwlovers.ai.dto.response;

import com.capstone.bwlovers.ai.dto.request.AiCallbackRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
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
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    }

    // callback -> 리스트 요약 변환
    public static AiRecommendationListResponse fromCallback(AiCallbackRequest callback) {

        AiRecommendationListResponse res = new AiRecommendationListResponse();
        if (callback == null) {
            res.setItems(Collections.emptyList());
            return res;
        }

        res.setResultId(callback.getResultId());
        res.setExpiresInSec(callback.getExpiresInSec());

        if (callback.getItems() == null || callback.getItems().isEmpty()) {
            res.setItems(Collections.emptyList());
            return res;
        }

        List<Item> listItems = new ArrayList<>();

        for (AiCallbackRequest.Item it : callback.getItems()) {
            if (it == null) continue;

            Item item = new Item();
            item.setItemId(it.getItemId());
            item.setInsuranceCompany(it.getInsuranceCompany());
            item.setProductName(it.getProductName());
            item.setIsLongTerm(it.getIsLongTerm());
            item.setMonthlyCost(it.getMonthlyCost());
            listItems.add(item);
        }

        res.setItems(listItems);
        return res;
    }
}
