package com.capstone.bwlovers.ai.recommendation.dto.response;

import com.capstone.bwlovers.ai.recommendation.dto.request.RecommendationCallbackRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class RecommendationListResponse {

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

        @JsonProperty("sum_insured")
        private Long sumInsured;

        @JsonProperty("monthly_cost")
        private String monthlyCost;

        @JsonProperty("insurance_recommendation_reason")
        private String insuranceRecommendationReason;

        @JsonProperty("special_contracts")
        private List<SpecialContract> specialContracts;

        @JsonProperty("evidence_sources")
        private List<EvidenceSource> evidenceSources;

        @JsonProperty("special_contract_count")
        private Integer specialContractCount = 0;

        /**
         * 응답 직전에 count를 무조건 현재 specialContracts 기준으로 보정함
         * - FastAPI가 special_contract_count를 잘못 주거나(0), 안 주거나(null) 상관없이
         * - special_contracts가 있으면 size로 맞춰짐
         */
        public void normalizeCounts() {
            if (this.specialContracts == null) {
                this.specialContractCount = 0;
            } else {
                this.specialContractCount = this.specialContracts.size();
            }
        }
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EvidenceSource {

        @JsonProperty("page_number")
        private Integer pageNumber;

        @JsonProperty("text_snippet")
        private String textSnippet;
    }

    // =========================================================
    // callback -> 리스트 변환
    // =========================================================
    public static RecommendationListResponse fromCallback(RecommendationCallbackRequest callback) {

        RecommendationListResponse res = new RecommendationListResponse();

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

        for (RecommendationCallbackRequest.Item it : callback.getItems()) {
            if (it == null) continue;

            Item item = new Item();
            item.setItemId(it.getItemId());
            item.setInsuranceCompany(it.getInsuranceCompany());
            item.setProductName(it.getProductName());
            item.setIsLongTerm(it.getIsLongTerm());
            item.setSumInsured(it.getSumInsured());
            item.setMonthlyCost(it.getMonthlyCost());
            item.setInsuranceRecommendationReason(it.getInsuranceRecommendationReason());

            // special_contracts 변환
            if (it.getSpecialContracts() != null && !it.getSpecialContracts().isEmpty()) {
                List<SpecialContract> contracts = it.getSpecialContracts().stream()
                        .map(sc -> {
                            SpecialContract c = new SpecialContract();
                            c.setContractName(sc.getContractName());
                            c.setContractDescription(sc.getContractDescription());
                            c.setContractRecommendationReason(sc.getContractRecommendationReason());
                            c.setKeyFeatures(sc.getKeyFeatures());
                            c.setPageNumber(sc.getPageNumber());
                            return c;
                        })
                        .toList();
                item.setSpecialContracts(contracts);
            } else {
                item.setSpecialContracts(Collections.emptyList());
            }

            // evidence_sources 변환
            if (it.getEvidenceSources() != null && !it.getEvidenceSources().isEmpty()) {
                List<EvidenceSource> sources = it.getEvidenceSources().stream()
                        .map(es -> {
                            EvidenceSource e = new EvidenceSource();
                            e.setPageNumber(es.getPageNumber());
                            e.setTextSnippet(es.getTextSnippet());
                            return e;
                        })
                        .toList();
                item.setEvidenceSources(sources);
            } else {
                item.setEvidenceSources(Collections.emptyList());
            }

            // count는 무조건 현재 specialContracts 기준으로 보정
            item.normalizeCounts();

            listItems.add(item);
        }

        res.setItems(listItems);
        return res;
    }

    /**
     * FastAPI 응답을 그대로 파싱해서 받은 경우에도 count를 보정할 수 있게 유틸 제공함
     * - Service에서 list 파싱 후 이 메서드 호출하면 됨
     */
    public void normalizeAllCounts() {
        if (this.items == null) return;
        for (Item it : this.items) {
            if (it == null) continue;
            it.normalizeCounts();
        }
    }
}
