package com.capstone.bwlovers.ai.dto.response;

import com.capstone.bwlovers.ai.dto.request.AiCallbackRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AiRecommendationResponse {

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

    // callback item -> 상세 응답 변환
    public static AiRecommendationResponse fromCallbackItem(AiCallbackRequest.Item item) {

        AiRecommendationResponse res = new AiRecommendationResponse();

        res.setInsuranceCompany(item.getInsuranceCompany());
        res.setProductName(item.getProductName());
        res.setLongTerm(Boolean.TRUE.equals(item.getIsLongTerm()));
        res.setMonthlyCost(item.getMonthlyCost());
        res.setInsuranceRecommendationReason(item.getInsuranceRecommendationReason());

        // special_contracts 변환
        if (item.getSpecialContracts() != null) {
            List<AiRecommendationResponse.SpecialContract> contracts = item.getSpecialContracts().stream()
                    .map(sc -> {
                        AiRecommendationResponse.SpecialContract c = new AiRecommendationResponse.SpecialContract();
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
            List<AiRecommendationResponse.EvidenceSource> sources = item.getEvidenceSources().stream()
                    .map(es -> {
                        AiRecommendationResponse.EvidenceSource e = new AiRecommendationResponse.EvidenceSource();
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
