package com.capstone.bwlovers.ai.recommendation.dto.response;

import com.capstone.bwlovers.ai.recommendation.dto.request.RecommendationCallbackRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RecommendationResponse {

    @JsonProperty("itemId")
    private String itemId;

    @JsonProperty("insurance_company")
    private String insuranceCompany;

    @JsonProperty("is_long_term")
    private boolean isLongTerm;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("insurance_recommendation_reason")
    private String insuranceRecommendationReason;

    @JsonProperty("sum_insured")
    private Long sumInsured;

    @JsonProperty("monthly_cost")
    private String monthlyCost;

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

    // =========================================================
    // Factory
    // =========================================================

    /**
     * callback item -> 상세 응답 변환
     */
    public static RecommendationResponse fromCallbackItem(RecommendationCallbackRequest.Item item) {

        RecommendationResponse res = new RecommendationResponse();
        res.setItemId(item.getItemId());
        res.setInsuranceCompany(item.getInsuranceCompany());
        res.setProductName(item.getProductName());
        res.setLongTerm(Boolean.TRUE.equals(item.getIsLongTerm()));
        res.setSumInsured(item.getSumInsured());
        res.setMonthlyCost(item.getMonthlyCost());
        res.setInsuranceRecommendationReason(item.getInsuranceRecommendationReason());

        // special_contracts 변환
        if (item.getSpecialContracts() != null) {
            List<RecommendationResponse.SpecialContract> contracts = item.getSpecialContracts().stream()
                    .map(sc -> {
                        RecommendationResponse.SpecialContract c = new RecommendationResponse.SpecialContract();
                        c.setContractName(sc.getContractName());
                        c.setContractDescription(sc.getContractDescription());
                        c.setContractRecommendationReason(sc.getContractRecommendationReason());
                        c.setKeyFeatures(sc.getKeyFeatures());
                        c.setPageNumber(sc.getPageNumber());
                        return c;
                    })
                    .toList();
            res.setSpecialContracts(contracts);
        }

        // evidence_sources 변환
        if (item.getEvidenceSources() != null) {
            List<RecommendationResponse.EvidenceSource> sources = item.getEvidenceSources().stream()
                    .map(es -> {
                        RecommendationResponse.EvidenceSource e = new RecommendationResponse.EvidenceSource();
                        e.setPageNumber(es.getPageNumber());
                        e.setTextSnippet(es.getTextSnippet());
                        return e;
                    })
                    .toList();
            res.setEvidenceSources(sources);
        }

        return res;
    }

    /**
     * 리스트 item -> "임시 상세" 변환
     */
    // 리스트 item -> 상세 응답 변환 (리스트에 상세가 내려오면 그대로 채움)
    public static RecommendationResponse fromListItem(RecommendationListResponse.Item item) {

        RecommendationResponse res = new RecommendationResponse();
        res.setItemId(item.getItemId());
        res.setInsuranceCompany(item.getInsuranceCompany());
        res.setProductName(item.getProductName());
        res.setLongTerm(Boolean.TRUE.equals(item.getIsLongTerm()));
        res.setSumInsured(item.getSumInsured());
        res.setMonthlyCost(item.getMonthlyCost());
        res.setInsuranceRecommendationReason(item.getInsuranceRecommendationReason());

        // special_contracts (리스트에 있는 걸 그대로 상세에 매핑함)
        if (item.getSpecialContracts() != null) {
            List<RecommendationResponse.SpecialContract> contracts = item.getSpecialContracts().stream()
                    .map(sc -> {
                        RecommendationResponse.SpecialContract c = new RecommendationResponse.SpecialContract();
                        c.setContractName(sc.getContractName());
                        c.setContractDescription(sc.getContractDescription());
                        c.setContractRecommendationReason(sc.getContractRecommendationReason());
                        c.setKeyFeatures(sc.getKeyFeatures());
                        c.setPageNumber(sc.getPageNumber());
                        return c;
                    })
                    .toList();
            res.setSpecialContracts(contracts);
        }

        // evidence_sources (리스트에 있는 걸 그대로 상세에 매핑함)
        if (item.getEvidenceSources() != null) {
            List<RecommendationResponse.EvidenceSource> sources = item.getEvidenceSources().stream()
                    .map(es -> {
                        RecommendationResponse.EvidenceSource e = new RecommendationResponse.EvidenceSource();
                        e.setPageNumber(es.getPageNumber());
                        e.setTextSnippet(es.getTextSnippet());
                        return e;
                    })
                    .toList();
            res.setEvidenceSources(sources);
        }

        return res;
    }

}
