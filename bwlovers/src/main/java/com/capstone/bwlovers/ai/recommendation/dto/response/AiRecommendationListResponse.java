package com.capstone.bwlovers.ai.recommendation.dto.response;

import com.capstone.bwlovers.ai.recommendation.dto.request.AiCallbackRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiRecommendationListResponse {

    @JsonProperty("resultId")
    private String resultId;

    @JsonProperty("expiresInSec")
    private Integer expiresInSec;

    @JsonProperty("items")
    private List<Item> items;

    @JsonProperty("rag_metadata")
    private Map<String, Object> ragMetadata;

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

        public void normalizeCounts() {
            if (this.specialContracts == null) this.specialContractCount = 0;
            else this.specialContractCount = this.specialContracts.size();
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
            item.setSumInsured(it.getSumInsured());
            item.setMonthlyCost(it.getMonthlyCost());
            item.setInsuranceRecommendationReason(it.getInsuranceRecommendationReason());

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

            item.normalizeCounts();
            listItems.add(item);
        }

        res.setItems(listItems);
        return res;
    }

    public void normalizeAllCounts() {
        if (this.items == null) return;
        for (Item it : this.items) {
            if (it == null) continue;
            it.normalizeCounts();
        }
    }
}