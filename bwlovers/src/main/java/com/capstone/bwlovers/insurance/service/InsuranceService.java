package com.capstone.bwlovers.insurance.service;

import com.capstone.bwlovers.ai.recommendation.dto.response.RecommendationResponse;
import com.capstone.bwlovers.ai.recommendation.service.RecommendationCacheService;
import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.auth.repository.UserRepository;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.capstone.bwlovers.insurance.domain.InsuranceProduct;
import com.capstone.bwlovers.insurance.domain.SpecialContract;
import com.capstone.bwlovers.insurance.dto.request.InsuranceSelectionSaveRequest;
import com.capstone.bwlovers.insurance.dto.response.InsuranceDetailListResponse;
import com.capstone.bwlovers.insurance.dto.response.InsuranceDetailResponse;
import com.capstone.bwlovers.insurance.dto.response.InsuranceListResponse;
import com.capstone.bwlovers.insurance.repository.InsuranceProductRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InsuranceService {

    private final UserRepository userRepository;
    private final InsuranceProductRepository insuranceProductRepository;
    private final RecommendationCacheService recommendationCacheService;
    private final ObjectMapper objectMapper;

    @Transactional
    public Long saveSelected(Long userId, InsuranceSelectionSaveRequest request) {
        User user = findUser(userId);
        validateSaveRequest(request);

        RecommendationResponse detail = recommendationCacheService.getDetail(
                request.getResultId(),
                request.getItemId()
        );
        if (detail == null) {
            throw new CustomException(ExceptionCode.AI_RESULT_NOT_FOUND);
        }

        String company = nullToEmpty(detail.getInsuranceCompany());
        String productName = nullToEmpty(detail.getProductName());
        if (isBlank(company) || isBlank(productName)) {
            throw new CustomException(ExceptionCode.AI_PROCESSING_FAILED);
        }

        InsuranceProduct insurance = insuranceProductRepository
                .findByUser_UserIdAndResultIdAndItemId(userId, request.getResultId(), request.getItemId())
                .orElseGet(() -> createInsuranceProduct(user, request, detail, company, productName));

        int addedCount = addSelectedContracts(insurance, request, detail);

        if (addedCount == 0) {
            throw new CustomException(ExceptionCode.AI_SAVE_EMPTY_SELECTION);
        }

        return insuranceProductRepository.save(insurance).getInsuranceId();
    }

    @Transactional
    public String updateInsuranceMemo(Long userId, Long insuranceId, String newMemo) {
        InsuranceProduct insurance = getOwnedInsurance(userId, insuranceId);
        insurance.updateMemo(newMemo);
        return insurance.getMemo();
    }

    @Transactional
    public void deleteInsurance(Long userId, Long insuranceId) {
        insuranceProductRepository.delete(getOwnedInsurance(userId, insuranceId));
    }

    @Transactional(readOnly = true)
    public List<InsuranceListResponse> getMyInsuranceList(Long userId) {
        List<InsuranceProduct> products = insuranceProductRepository.findAllByUser_UserIdOrderByCreatedAtDesc(userId);

        return products.stream()
                .map(product -> InsuranceListResponse.builder()
                        .insuranceId(product.getInsuranceId())
                        .insuranceCompany(product.getInsuranceCompany())
                        .productName(product.getProductName())
                        .createdAt(product.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InsuranceDetailListResponse> getMyInsuranceDetails(Long userId) {
        List<InsuranceProduct> products = insuranceProductRepository.findAllByUserIdWithContracts(userId);

        return products.stream()
                .map(product -> InsuranceDetailListResponse.builder()
                        .insuranceId(product.getInsuranceId())
                        .insuranceCompany(product.getInsuranceCompany())
                        .productName(product.getProductName())
                        .isLongTerm(product.isLongTerm())
                        .sumInsured(product.getSumInsured())
                        .monthlyCost(product.getMonthlyCost())
                        .memo(product.getMemo())
                        .createdAt(product.getCreatedAt())
                        .specialContracts(
                                product.getSpecialContracts().stream()
                                        .map(sc -> InsuranceDetailListResponse.SpecialContractResponse.builder()
                                                .contractId(sc.getContractId())
                                                .contractName(sc.getContractName())
                                                .build()
                                        )
                                        .toList()
                        )
                        .build()
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public InsuranceDetailResponse getInsuranceDetail(Long userId, Long insuranceId) {
        InsuranceProduct insurance = getOwnedInsuranceWithContracts(userId, insuranceId);

        List<InsuranceDetailResponse.SpecialContractDetailDto> contractDtos = insurance.getSpecialContracts().stream()
                .map(sc -> InsuranceDetailResponse.SpecialContractDetailDto.builder()
                        .contractId(sc.getContractId())
                        .contractName(sc.getContractName())
                        .contractDescription(sc.getContractDescription())
                        .contractRecommendationReason(sc.getContractRecommendationReason())
                        .keyFeatures(parseJsonList(sc.getKeyFeatures()))
                        .pageNumber(sc.getPageNumber())
                        .build())
                .toList();

        return InsuranceDetailResponse.builder()
                .insuranceId(insurance.getInsuranceId())
                .resultId(insurance.getResultId())
                .itemId(insurance.getItemId())
                .insuranceCompany(insurance.getInsuranceCompany())
                .productName(insurance.getProductName())
                .isLongTerm(insurance.isLongTerm())
                .sumInsured(insurance.getSumInsured())
                .monthlyCost(insurance.getMonthlyCost())
                .insuranceRecommendationReason(insurance.getInsuranceRecommendationReason())
                .memo(insurance.getMemo())
                .specialContracts(contractDtos)
                .createdAt(insurance.getCreatedAt())
                .build();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
    }

    private void validateSaveRequest(InsuranceSelectionSaveRequest request) {
        if (request == null || isBlank(request.getResultId()) || isBlank(request.getItemId())) {
            throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        }

        if (request.getSelectedContractNames() == null || request.getSelectedContractNames().isEmpty()) {
            throw new CustomException(ExceptionCode.AI_SAVE_EMPTY_SELECTION);
        }
    }

    private InsuranceProduct createInsuranceProduct(
            User user,
            InsuranceSelectionSaveRequest request,
            RecommendationResponse detail,
            String company,
            String productName
    ) {
        return insuranceProductRepository.save(
                InsuranceProduct.builder()
                        .user(user)
                        .resultId(request.getResultId())
                        .itemId(request.getItemId())
                        .insuranceCompany(company)
                        .productName(productName)
                        .isLongTerm(detail.isLongTerm())
                        .sumInsured(detail.getSumInsured() == null ? "0" : detail.getSumInsured())
                        .monthlyCost(detail.getMonthlyCost() == null ? "0" : detail.getMonthlyCost())
                        .insuranceRecommendationReason(nullToEmpty(detail.getInsuranceRecommendationReason()))
                        .memo(nullToEmpty(request.getMemo()))
                        .build()
        );
    }

    private int addSelectedContracts(
            InsuranceProduct insurance,
            InsuranceSelectionSaveRequest request,
            RecommendationResponse detail
    ) {
        if (detail.getSpecialContracts() == null || detail.getSpecialContracts().isEmpty()) {
            throw new CustomException(ExceptionCode.AI_PROCESSING_FAILED);
        }

        Set<String> selectedNames = new HashSet<>(request.getSelectedContractNames());
        Set<String> existingNames = extractExistingContractNames(insurance);
        int addedCount = 0;

        for (RecommendationResponse.SpecialContract specialContract : detail.getSpecialContracts()) {
            if (specialContract == null || isBlank(specialContract.getContractName())) {
                continue;
            }
            if (!selectedNames.contains(specialContract.getContractName())) {
                continue;
            }
            if (existingNames.contains(specialContract.getContractName())) {
                continue;
            }

            insurance.addContract(buildSpecialContract(specialContract));
            existingNames.add(specialContract.getContractName());
            addedCount++;
        }

        return addedCount;
    }

    private Set<String> extractExistingContractNames(InsuranceProduct insurance) {
        Set<String> existingNames = new HashSet<>();
        if (insurance.getSpecialContracts() == null) {
            return existingNames;
        }

        for (SpecialContract contract : insurance.getSpecialContracts()) {
            if (contract != null && contract.getContractName() != null) {
                existingNames.add(contract.getContractName());
            }
        }
        return existingNames;
    }

    private SpecialContract buildSpecialContract(RecommendationResponse.SpecialContract specialContract) {
        String keyFeaturesJson = toJson(specialContract.getKeyFeatures());

        return SpecialContract.builder()
                .contractName(specialContract.getContractName())
                .contractDescription(nullToEmpty(specialContract.getContractDescription()))
                .contractRecommendationReason(nullToEmpty(specialContract.getContractRecommendationReason()))
                .keyFeatures(keyFeaturesJson == null ? "[]" : keyFeaturesJson)
                .pageNumber(specialContract.getPageNumber() == null ? 0L : specialContract.getPageNumber().longValue())
                .build();
    }

    private InsuranceProduct getOwnedInsurance(Long userId, Long insuranceId) {
        InsuranceProduct insurance = insuranceProductRepository.findById(insuranceId)
                .orElseThrow(() -> new CustomException(ExceptionCode.INSURANCE_NOT_FOUND));
        validateOwnership(userId, insurance);
        return insurance;
    }

    private InsuranceProduct getOwnedInsuranceWithContracts(Long userId, Long insuranceId) {
        InsuranceProduct insurance = insuranceProductRepository.findByInsuranceIdWithContracts(insuranceId)
                .orElseThrow(() -> new CustomException(ExceptionCode.INSURANCE_NOT_FOUND));
        validateOwnership(userId, insurance);
        return insurance;
    }

    private void validateOwnership(Long userId, InsuranceProduct insurance) {
        if (!insurance.getUser().getUserId().equals(userId)) {
            throw new CustomException(ExceptionCode.USER_NOT_FOUND);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new CustomException(ExceptionCode.JSON_SERIALIZATION_FAILED);
        }
    }

    private List<String> parseJsonList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
