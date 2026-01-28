package com.capstone.bwlovers.ai.service;

import com.capstone.bwlovers.ai.dto.request.AiSaveSelectedRequest;
import com.capstone.bwlovers.ai.dto.request.FastApiRequest;
import com.capstone.bwlovers.ai.dto.response.AiRecommendationListResponse;
import com.capstone.bwlovers.ai.dto.response.FastApiResponse;
import com.capstone.bwlovers.ai.dto.response.AiRecommendationResponse;
import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.auth.repository.UserRepository;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.capstone.bwlovers.health.domain.HealthStatus;
import com.capstone.bwlovers.health.dto.request.HealthStatusRequest;
import com.capstone.bwlovers.health.repository.HealthStatusRepository;
import com.capstone.bwlovers.insurance.domain.InsuranceProduct;
import com.capstone.bwlovers.insurance.domain.SpecialContract;
import com.capstone.bwlovers.insurance.repository.InsuranceProductRepository;
import com.capstone.bwlovers.pregnancy.domain.PregnancyInfo;
import com.capstone.bwlovers.pregnancy.dto.request.PregnancyInfoRequest;
import com.capstone.bwlovers.pregnancy.repository.PregnancyInfoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AiService {

    private final UserRepository userRepository;
    private final PregnancyInfoRepository pregnancyInfoRepository;
    private final HealthStatusRepository healthStatusRepository;
    private final WebClient aiWebClient;

    private final InsuranceProductRepository insuranceProductRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * (기존 A안) 결과 전체를 바로 받는 방식 - 유지함(선택)
     */
    public FastApiResponse requestAiRecommendation(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
        PregnancyInfo pregnancyInfo = pregnancyInfoRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ExceptionCode.PREGNANCY_INFO_NOT_FOUND));
        HealthStatus healthStatus = healthStatusRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ExceptionCode.HEALTH_STATUS_NOT_FOUND));

        FastApiRequest dto = toFastApiRequest(pregnancyInfo, healthStatus);

        return aiWebClient.post()
                .uri("/ai/recommend")
                .bodyValue(dto)
                .retrieve()
                .onStatus(s -> s.value() == 400 || s.value() == 422,
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_INVALID_REQUEST)))
                .onStatus(s -> s.value() == 409,
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_PROCESSING_FAILED)))
                .onStatus(s -> s.is5xxServerError(),
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_SERVER_5XX)))
                .bodyToMono(FastApiResponse.class)
                .timeout(Duration.ofSeconds(25))
                .block();
    }

    // =========================================================
    // 리스트 → 상세 보기 → 선택 저장
    // =========================================================

    /**
     * 추천 리스트 조회
     */
    public AiRecommendationListResponse requestAiRecommendationList(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
        PregnancyInfo pregnancyInfo = pregnancyInfoRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ExceptionCode.PREGNANCY_INFO_NOT_FOUND));
        HealthStatus healthStatus = healthStatusRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ExceptionCode.HEALTH_STATUS_NOT_FOUND));

        FastApiRequest dto = toFastApiRequest(pregnancyInfo, healthStatus);

        AiRecommendationListResponse list = aiWebClient.post()
                .uri("/ai/recommend") // FastAPI: 리스트 + resultId 반환해야 함
                .bodyValue(dto)
                .retrieve()
                .onStatus(s -> s.value() == 400 || s.value() == 422,
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_INVALID_REQUEST)))
                .onStatus(s -> s.value() == 409,
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_PROCESSING_FAILED)))
                .onStatus(s -> s.is5xxServerError(),
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_SERVER_5XX)))
                .bodyToMono(AiRecommendationListResponse.class)
                .timeout(Duration.ofSeconds(25))
                .block();

        if (list == null || isBlank(list.getResultId())) {
            throw new CustomException(ExceptionCode.AI_SERVER_5XX);
        }
        if (list.getItems() == null || list.getItems().isEmpty()) {
            throw new CustomException(ExceptionCode.AI_PROCESSING_FAILED);
        }
        return list;
    }

    /**
     * 결과 상세 보기
     * GET /ai/results/{resultId}/items/{itemId}
     */
    public AiRecommendationResponse fetchAiResultDetail(Long userId, String resultId, String itemId) {

        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        if (isBlank(resultId) || isBlank(itemId)) {
            throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        }

        return aiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ai/results/{resultId}/items/{itemId}")
                        .build(resultId, itemId))
                .retrieve()
                .onStatus(s -> s.value() == 404,
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_RESULT_NOT_FOUND)))
                .onStatus(s -> s.is5xxServerError(),
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_SERVER_5XX)))
                .bodyToMono(AiRecommendationResponse.class)
                .timeout(Duration.ofSeconds(10))
                .block();
    }

    /**
     * 선택 저장
     *
     * 동일 보험 기준:
     * - 보험회사명 + 보험이름 + 장기여부가 같으면 같은 보험으로 판단함
     *
     * 동작:
     * 1) (resultId, itemId)로 AI 상세 결과 가져옴
     * 2) 동일 보험이 DB에 있으면 그 보험(insuranceId)에 특약을 누적 저장함
     * 3) 없으면 보험을 새로 만든 뒤 특약을 저장함
     */
    @Transactional
    public Long saveSelected(Long userId, AiSaveSelectedRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        if (request == null || isBlank(request.getResultId()) || isBlank(request.getItemId())) {
            throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        }
        if (request.getSelectedContractNames() == null || request.getSelectedContractNames().isEmpty()) {
            throw new CustomException(ExceptionCode.AI_SAVE_EMPTY_SELECTION);
        }

        // AI 상세 결과 조회
        AiRecommendationResponse detail =
                fetchAiResultDetail(userId, request.getResultId(), request.getItemId());

        String company = nullToEmpty(detail.getInsuranceCompany());
        String productName = nullToEmpty(detail.getProductName());
        boolean isLongTerm = detail.isLongTerm();

        if (isBlank(company) || isBlank(productName)) {
            throw new CustomException(ExceptionCode.AI_PROCESSING_FAILED);
        }

        // 동일 보험 찾기: (user + company + productName + isLongTerm)
        InsuranceProduct insurance = insuranceProductRepository
                .findByUser_UserIdAndInsuranceCompanyAndProductNameAndIsLongTerm(
                        userId, company, productName, isLongTerm
                )
                .orElseGet(() -> insuranceProductRepository.save(
                        InsuranceProduct.builder()
                                .user(user)
                                .resultId(request.getResultId())
                                .insuranceCompany(company)
                                .productName(productName)
                                .isLongTerm(isLongTerm)
                                .monthlyCost(detail.getMonthlyCost() == null ? 0L : detail.getMonthlyCost().longValue())
                                .insuranceRecommendationReason(nullToEmpty(detail.getInsuranceRecommendationReason()))
                                .build()
                ));

        // 선택한 특약만 누적 추가
        Set<String> selected = new HashSet<>(request.getSelectedContractNames());

        if (detail.getSpecialContracts() == null || detail.getSpecialContracts().isEmpty()) {
            throw new CustomException(ExceptionCode.AI_PROCESSING_FAILED);
        }

        // 중복 방지: 이미 들어있는 특약명은 스킵함
        Set<String> existingNames = new HashSet<>();
        if (insurance.getSpecialContracts() != null) {
            for (SpecialContract c : insurance.getSpecialContracts()) {
                if (c != null && c.getContractName() != null) {
                    existingNames.add(c.getContractName());
                }
            }
        }

        for (var sc : detail.getSpecialContracts()) {
            if (sc == null || sc.getContractName() == null) continue;
            if (!selected.contains(sc.getContractName())) continue;

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
        }

        // "이번 요청에서 선택된 특약"이 하나도 추가되지 않으면 저장 의미 없으니 에러 처리함
        // (기존 보험에 이미 다 들어있어서 스킵된 케이스 포함)
        if (insurance.getSpecialContracts() == null || insurance.getSpecialContracts().isEmpty()) {
            throw new CustomException(ExceptionCode.AI_SAVE_EMPTY_SELECTION);
        }

        InsuranceProduct saved = insuranceProductRepository.save(insurance);
        return saved.getInsuranceId();
    }


    // =========================================================
    // private
    // =========================================================

    private FastApiRequest toFastApiRequest(PregnancyInfo pregnancyInfo, HealthStatus healthStatus) {
        PregnancyInfoRequest pregnancyInfoRequest = PregnancyInfoRequest.from(pregnancyInfo);
        HealthStatusRequest healthStatusRequest = HealthStatusRequest.from(healthStatus);
        return new FastApiRequest(pregnancyInfoRequest, healthStatusRequest);
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
}
