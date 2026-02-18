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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InsuranceService {

    private final UserRepository userRepository;
    private final InsuranceProductRepository insuranceProductRepository;

    //  캐시에서 상세 가져오기용 (AI 서버 콜 안 하고 Redis 사용)
    private final RecommendationCacheService recommendationCacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /*
    보험 저장
     */
    @Transactional
    public Long saveSelected(Long userId, InsuranceSelectionSaveRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        if (request == null
                || isBlank(request.getResultId())
                || isBlank(request.getItemId())) {
            throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        }

        if (request.getSelectedContractNames() == null || request.getSelectedContractNames().isEmpty()) {
            throw new CustomException(ExceptionCode.AI_SAVE_EMPTY_SELECTION);
        }

        // ============================
        // Redis에서 AI 상세 결과 가져오기
        // ============================
        RecommendationResponse detail =
                recommendationCacheService.getDetail(request.getResultId(), request.getItemId());

        if (detail == null) {
            // 너가 이미 쓰는 코드: AI_RESULT_NOT_FOUND
            throw new CustomException(ExceptionCode.AI_RESULT_NOT_FOUND);
        }

        String company = nullToEmpty(detail.getInsuranceCompany());
        String productName = nullToEmpty(detail.getProductName());
        boolean isLongTerm = detail.isLongTerm();

        if (isBlank(company) || isBlank(productName)) {
            throw new CustomException(ExceptionCode.AI_PROCESSING_FAILED);
        }

        // 동일 보험 찾기: (user + ResultId + ItemId)
        InsuranceProduct insurance = insuranceProductRepository
                .findByUser_UserIdAndResultIdAndItemId(userId, request.getResultId(), request.getItemId())
                .orElseGet(() -> insuranceProductRepository.save(
                        InsuranceProduct.builder()
                                .user(user)
                                .resultId(request.getResultId())
                                .itemId(request.getItemId())
                                .insuranceCompany(company)
                                .productName(productName)
                                .isLongTerm(isLongTerm)
                                .sumInsured(detail.getSumInsured() == null ? 0L : detail.getSumInsured().longValue())
                                .monthlyCost(detail.getMonthlyCost() == null ? "0" : String.valueOf(detail.getMonthlyCost()))
                                .insuranceRecommendationReason(nullToEmpty(detail.getInsuranceRecommendationReason()))
                                .memo(nullToEmpty(request.getMemo()))
                                .build()
                ));


        // 선택한 특약만 추가
        Set<String> selectedNames = new HashSet<>(request.getSelectedContractNames());

        if (detail.getSpecialContracts() == null || detail.getSpecialContracts().isEmpty()) {
            throw new CustomException(ExceptionCode.AI_PROCESSING_FAILED);
        }

        // 이미 저장된 특약명 중복 방지
        Set<String> existingNames = new HashSet<>();
        if (insurance.getSpecialContracts() != null) {
            for (SpecialContract c : insurance.getSpecialContracts()) {
                if (c != null && c.getContractName() != null) {
                    existingNames.add(c.getContractName());
                }
            }
        }

        int addedCount = 0;

        for (var sc : detail.getSpecialContracts()) {
            if (sc == null || isBlank(sc.getContractName())) continue;

            // 이번 요청에서 선택한 특약만
            if (!selectedNames.contains(sc.getContractName())) continue;

            // 이미 있으면 스킵
            if (existingNames.contains(sc.getContractName())) continue;

            String keyFeaturesJson = toJson(sc.getKeyFeatures());

            SpecialContract contract = SpecialContract.builder()
                    .contractName(sc.getContractName())
                    .contractDescription(nullToEmpty(sc.getContractDescription()))
                    .contractRecommendationReason(nullToEmpty(sc.getContractRecommendationReason()))
                    .keyFeatures(keyFeaturesJson == null ? "[]" : keyFeaturesJson)
                    .pageNumber(sc.getPageNumber() == null ? 0L : sc.getPageNumber().longValue())
                    .build();

            insurance.addContract(contract);
            existingNames.add(sc.getContractName());
            addedCount++;
        }

        if (addedCount == 0) {
            throw new CustomException(ExceptionCode.AI_SAVE_EMPTY_SELECTION);
        }

        InsuranceProduct saved = insuranceProductRepository.save(insurance);
        return saved.getInsuranceId();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new CustomException(ExceptionCode.JSON_SERIALIZATION_FAILED);
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /*
    메모 수정
     */
    @Transactional
    public String updateInsuranceMemo(Long userId, Long insuranceId, String newMemo) {
        InsuranceProduct insurance = insuranceProductRepository.findById(insuranceId)
                .orElseThrow(() -> new CustomException(ExceptionCode.INSURANCE_NOT_FOUND));
        if (!insurance.getUser().getUserId().equals(userId)) {
            throw new CustomException(ExceptionCode.USER_NOT_FOUND);
        }

        insurance.updateMemo(newMemo);
        return insurance.getMemo();
    }

    /*
    보험 삭제
     */
    @Transactional
    public void deleteInsurance(Long userId, Long insuranceId) {
        InsuranceProduct insurance = insuranceProductRepository.findById(insuranceId)
                .orElseThrow(() -> new CustomException(ExceptionCode.INSURANCE_NOT_FOUND));
        if (!insurance.getUser().getUserId().equals(userId)) {
            throw new CustomException(ExceptionCode.USER_NOT_FOUND);
        }

        insuranceProductRepository.delete(insurance);
    }

    /*
    마이페이지 보험 리스트 조회
     */
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
                .collect(Collectors.toList());
    }

    /*
    마이페이지 보험 상세 리스트 조회
     */
    @Transactional(readOnly = true)
    public List<InsuranceDetailListResponse> getMyInsuranceDetails(Long userId) {
        List<InsuranceProduct> products = insuranceProductRepository.findAllByUser_UserIdOrderByCreatedAtDesc(userId);

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
                        .specialContractNames(product.getSpecialContracts().stream()
                                .map(sc -> sc.getContractName())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    /*
    마이페이지 보험 상세보기
     */
    @Transactional(readOnly = true)
    public InsuranceDetailResponse getInsuranceDetail(Long userId, Long insuranceId) {
        InsuranceProduct insurance = insuranceProductRepository.findById(insuranceId)
                .orElseThrow(() -> new CustomException(ExceptionCode.INSURANCE_NOT_FOUND));
        if (!insurance.getUser().getUserId().equals(userId)) {
            throw new CustomException(ExceptionCode.USER_NOT_FOUND);
        }

        List<InsuranceDetailResponse.SpecialContractDetailDto> contractDtos = insurance.getSpecialContracts().stream()
                .map(sc -> InsuranceDetailResponse.SpecialContractDetailDto.builder()
                        .contractId(sc.getContractId())
                        .contractName(sc.getContractName())
                        .contractDescription(sc.getContractDescription())
                        .contractRecommendationReason(sc.getContractRecommendationReason())
                        .keyFeatures(parseJsonList(sc.getKeyFeatures()))
                        .pageNumber(sc.getPageNumber())
                        .build())
                .collect(Collectors.toList());

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
                .build();
    }

    // JSON 문자열을 리스트로 안전하게 변환하는 헬퍼 메서드
    private List<String> parseJsonList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of(); // 파싱 실패 시 빈 리스트 반환
        }
    }
}
