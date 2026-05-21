package com.capstone.bwlovers.ai.recommendation.dto.response;

import com.capstone.bwlovers.ai.recommendation.dto.request.RecommendationCallbackRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
        private String sumInsured;

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
         * 특약 수를 현재 specialContracts 기준으로 보정합니다.
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

    public static RecommendationListResponse fromCallback(RecommendationCallbackRequest callback) {
        RecommendationListResponse response = new RecommendationListResponse();

        if (callback == null) {
            response.setItems(Collections.emptyList());
            return response;
        }

        response.setResultId(callback.getResultId());
        response.setExpiresInSec(callback.getExpiresInSec());

        if (callback.getItemsOrEmpty().isEmpty()) {
            response.setItems(Collections.emptyList());
            return response;
        }

        List<Item> listItems = new ArrayList<>();

        for (RecommendationCallbackRequest.Item item : callback.getItemsOrEmpty()) {
            if (item == null) {
                continue;
            }
            listItems.add(toItem(item));
        }

        response.setItems(listItems);
        return response;
    }

    /**
     * FastAPI 응답을 그대로 파싱한 뒤에도 특약 수를 재보정할 수 있습니다.
     */
    public void normalizeAllCounts() {
        if (this.items == null) {
            return;
        }

        this.items.stream()
                .filter(Objects::nonNull)
                .forEach(Item::normalizeCounts);
    }

    private static Item toItem(RecommendationCallbackRequest.Item source) {
        Item item = new Item();
        item.setItemId(source.getItemId());
        item.setInsuranceCompany(source.getInsuranceCompany());
        item.setProductName(source.getProductName());
        item.setIsLongTerm(source.getIsLongTerm());
        item.setSumInsured(source.getSumInsured());
        item.setMonthlyCost(source.getMonthlyCost());
        item.setInsuranceRecommendationReason(source.getInsuranceRecommendationReason());
        item.setSpecialContracts(toSpecialContracts(source.getSpecialContracts()));
        item.setEvidenceSources(toEvidenceSources(source.getEvidenceSources()));
        item.normalizeCounts();
        return item;
    }

    private static List<SpecialContract> toSpecialContracts(
            List<RecommendationCallbackRequest.SpecialContract> source
    ) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }

        return source.stream()
                .map(RecommendationListResponse::toSpecialContract)
                .toList();
    }

    private static List<EvidenceSource> toEvidenceSources(
            List<RecommendationCallbackRequest.EvidenceSource> source
    ) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }

        return source.stream()
                .map(RecommendationListResponse::toEvidenceSource)
                .toList();
    }

    private static SpecialContract toSpecialContract(RecommendationCallbackRequest.SpecialContract source) {
        SpecialContract contract = new SpecialContract();
        contract.setContractName(source.getContractName());
        contract.setContractDescription(source.getContractDescription());
        contract.setContractRecommendationReason(source.getContractRecommendationReason());
        contract.setKeyFeatures(source.getKeyFeatures());
        contract.setPageNumber(source.getPageNumber());
        return contract;
    }

    private static EvidenceSource toEvidenceSource(RecommendationCallbackRequest.EvidenceSource source) {
        EvidenceSource evidenceSource = new EvidenceSource();
        evidenceSource.setPageNumber(source.getPageNumber());
        evidenceSource.setTextSnippet(source.getTextSnippet());
        return evidenceSource;
    }
}
