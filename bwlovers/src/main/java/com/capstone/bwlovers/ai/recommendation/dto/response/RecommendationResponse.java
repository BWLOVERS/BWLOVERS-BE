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
    private String sumInsured;

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

    /**
     * 콜백 item을 상세 응답으로 변환합니다.
     */
    public static RecommendationResponse fromCallbackItem(RecommendationCallbackRequest.Item item) {
        RecommendationResponse response = createBaseResponse(
                item.getItemId(),
                item.getInsuranceCompany(),
                item.getProductName(),
                item.getIsLongTerm(),
                item.getSumInsured(),
                item.getMonthlyCost(),
                item.getInsuranceRecommendationReason()
        );

        if (item.getSpecialContracts() != null) {
            response.setSpecialContracts(item.getSpecialContracts().stream()
                    .map(RecommendationResponse::toSpecialContract)
                    .toList());
        }

        if (item.getEvidenceSources() != null) {
            response.setEvidenceSources(item.getEvidenceSources().stream()
                    .map(RecommendationResponse::toEvidenceSource)
                    .toList());
        }

        return response;
    }

    /**
     * 리스트 item을 상세 응답 형태로 변환합니다.
     */
    public static RecommendationResponse fromListItem(RecommendationListResponse.Item item) {
        RecommendationResponse response = createBaseResponse(
                item.getItemId(),
                item.getInsuranceCompany(),
                item.getProductName(),
                item.getIsLongTerm(),
                item.getSumInsured(),
                item.getMonthlyCost(),
                item.getInsuranceRecommendationReason()
        );

        if (item.getSpecialContracts() != null) {
            response.setSpecialContracts(item.getSpecialContracts().stream()
                    .map(RecommendationResponse::toSpecialContract)
                    .toList());
        }

        if (item.getEvidenceSources() != null) {
            response.setEvidenceSources(item.getEvidenceSources().stream()
                    .map(RecommendationResponse::toEvidenceSource)
                    .toList());
        }

        return response;
    }

    private static RecommendationResponse createBaseResponse(
            String itemId,
            String insuranceCompany,
            String productName,
            Boolean isLongTerm,
            String sumInsured,
            String monthlyCost,
            String insuranceRecommendationReason
    ) {
        RecommendationResponse response = new RecommendationResponse();
        response.setItemId(itemId);
        response.setInsuranceCompany(insuranceCompany);
        response.setProductName(productName);
        response.setLongTerm(Boolean.TRUE.equals(isLongTerm));
        response.setSumInsured(sumInsured);
        response.setMonthlyCost(monthlyCost);
        response.setInsuranceRecommendationReason(insuranceRecommendationReason);
        return response;
    }

    private static RecommendationResponse.SpecialContract toSpecialContract(
            RecommendationCallbackRequest.SpecialContract source
    ) {
        RecommendationResponse.SpecialContract contract = new RecommendationResponse.SpecialContract();
        contract.setContractName(source.getContractName());
        contract.setContractDescription(source.getContractDescription());
        contract.setContractRecommendationReason(source.getContractRecommendationReason());
        contract.setKeyFeatures(source.getKeyFeatures());
        contract.setPageNumber(source.getPageNumber());
        return contract;
    }

    private static RecommendationResponse.SpecialContract toSpecialContract(
            RecommendationListResponse.SpecialContract source
    ) {
        RecommendationResponse.SpecialContract contract = new RecommendationResponse.SpecialContract();
        contract.setContractName(source.getContractName());
        contract.setContractDescription(source.getContractDescription());
        contract.setContractRecommendationReason(source.getContractRecommendationReason());
        contract.setKeyFeatures(source.getKeyFeatures());
        contract.setPageNumber(source.getPageNumber());
        return contract;
    }

    private static RecommendationResponse.EvidenceSource toEvidenceSource(
            RecommendationCallbackRequest.EvidenceSource source
    ) {
        RecommendationResponse.EvidenceSource evidenceSource = new RecommendationResponse.EvidenceSource();
        evidenceSource.setPageNumber(source.getPageNumber());
        evidenceSource.setTextSnippet(source.getTextSnippet());
        return evidenceSource;
    }

    private static RecommendationResponse.EvidenceSource toEvidenceSource(
            RecommendationListResponse.EvidenceSource source
    ) {
        RecommendationResponse.EvidenceSource evidenceSource = new RecommendationResponse.EvidenceSource();
        evidenceSource.setPageNumber(source.getPageNumber());
        evidenceSource.setTextSnippet(source.getTextSnippet());
        return evidenceSource;
    }
}
