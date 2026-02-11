package com.capstone.bwlovers.insurance.service;

import com.capstone.bwlovers.ai.dto.response.AiRecommendationResponse;
import com.capstone.bwlovers.ai.service.AiResultCacheService;
import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.auth.repository.UserRepository;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.capstone.bwlovers.insurance.domain.InsuranceProduct;
import com.capstone.bwlovers.insurance.domain.SpecialContract;
import com.capstone.bwlovers.insurance.dto.request.InsuranceSelectionSaveRequest;
import com.capstone.bwlovers.insurance.repository.InsuranceProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InsuranceService {

    private final UserRepository userRepository;
    private final InsuranceProductRepository insuranceProductRepository;

    //  캐시에서 상세 가져오기용 (AI 서버 콜 안 하고 Redis 사용)
    private final AiResultCacheService aiResultCacheService;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
        AiRecommendationResponse detail =
                aiResultCacheService.getDetail(request.getResultId(), request.getItemId());

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
                                .monthlyCost(detail.getMonthlyCost() == null ? 0L : detail.getMonthlyCost().longValue())
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
        InsuranceProduct insurance = insuranceProductRepository.findById(insuranceId)
                .orElseThrow(() -> new CustomException(ExceptionCode.INSURANCE_NOT_FOUND));

        insurance.updateMemo(newMemo);
        return insurance.getMemo();
    }
}
