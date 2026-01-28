package com.capstone.bwlovers.ai.service;

import com.capstone.bwlovers.ai.dto.request.AiSaveSelectedRequest;
import com.capstone.bwlovers.ai.dto.request.FastApiRequest;
import com.capstone.bwlovers.ai.dto.response.AiRecommendTicketResponse;
import com.capstone.bwlovers.ai.dto.response.FastApiResponse;
import com.capstone.bwlovers.ai.dto.response.FastApiTicketResponse;
import com.capstone.bwlovers.ai.dto.response.InsuranceRecommendationResponse;
import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.auth.repository.UserRepository;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.capstone.bwlovers.health.domain.HealthStatus;
import com.capstone.bwlovers.health.dto.request.HealthStatusRequest;
import com.capstone.bwlovers.health.repository.HealthStatusRepository;
import com.capstone.bwlovers.insurance.domain.EvidenceSource;
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
     * 결과를 바로 받는 방식 (디버깅용)
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

                .onStatus(
                        status -> status.value() == 400 || status.value() == 422,
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new CustomException(ExceptionCode.AI_INVALID_REQUEST)))
                )
                .onStatus(
                        status -> status.value() == 409,
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new CustomException(ExceptionCode.AI_PROCESSING_FAILED)))
                )
                .onStatus(
                        status -> status.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new CustomException(ExceptionCode.AI_SERVER_5XX)))
                )

                .bodyToMono(FastApiResponse.class)
                .timeout(Duration.ofSeconds(25))
                .block();
    }

    // =========================================================
    // ticket 발급 -> 결과 조회 -> 선택 저장
    // =========================================================

    /**
     * FastAPI가 결과를 임시 저장하고 resultId(ticket)만 내려주는 방식
     */
    public AiRecommendTicketResponse requestAiRecommendationTicket(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
        PregnancyInfo pregnancyInfo = pregnancyInfoRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ExceptionCode.PREGNANCY_INFO_NOT_FOUND));
        HealthStatus healthStatus = healthStatusRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ExceptionCode.HEALTH_STATUS_NOT_FOUND));

        FastApiRequest dto = toFastApiRequest(pregnancyInfo, healthStatus);

        FastApiTicketResponse ticket = aiWebClient.post()
                .uri("/ai/recommend") // FastAPI가 ticket을 내려주는 엔드포인트로 맞춰야 함
                .bodyValue(dto)
                .retrieve()

                .onStatus(
                        status -> status.value() == 400 || status.value() == 422,
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_INVALID_REQUEST))
                )
                .onStatus(
                        status -> status.value() == 409,
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_PROCESSING_FAILED))
                )
                .onStatus(
                        status -> status.is5xxServerError(),
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_SERVER_5XX))
                )

                .bodyToMono(FastApiTicketResponse.class)
                .timeout(Duration.ofSeconds(25))
                .block();

        if (ticket == null || ticket.getResultId() == null || ticket.getResultId().isBlank()) {
            throw new CustomException(ExceptionCode.AI_SERVER_5XX);
        }

        int ttl = ticket.getExpiresInSec() != null ? ticket.getExpiresInSec() : 600;
        return new AiRecommendTicketResponse(ticket.getResultId(), ttl);
    }

    /**
     * resultId로 결과 조회함 (프론트 미리보기용)
     */
    public InsuranceRecommendationResponse fetchAiResult(Long userId, String resultId) {

        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        if (resultId == null || resultId.isBlank()) {
            throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        }

        return aiWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/ai/results/{resultId}").build(resultId))
                .retrieve()

                .onStatus(
                        status -> status.value() == 404,
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_RESULT_NOT_FOUND))
                )
                .onStatus(
                        status -> status.is5xxServerError(),
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_SERVER_5XX))
                )

                .bodyToMono(InsuranceRecommendationResponse.class)
                .timeout(Duration.ofSeconds(10))
                .block();
    }

    /**
     * 사용자가 선택한 특약만 DB에 저장함
     */
    @Transactional
    public Long saveSelected(Long userId, AiSaveSelectedRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        if (request == null || request.getResultId() == null || request.getResultId().isBlank()) {
            throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        }
        if (request.getSelectedContractNames() == null || request.getSelectedContractNames().isEmpty()) {
            throw new CustomException(ExceptionCode.AI_SAVE_EMPTY_SELECTION);
        }

        // 중복 저장 방지함 (같은 resultId를 또 저장 못하게 함)
        insuranceProductRepository.findByUser_UserIdAndResultId(userId, request.getResultId())
                .ifPresent(x -> { throw new CustomException(ExceptionCode.AI_ALREADY_SAVED); }); // ✅ 추가 추천

        // FastAPI 임시 결과 가져옴
        InsuranceRecommendationResponse result = fetchAiResult(userId, request.getResultId());

        // 보험 저장 엔티티 생성
        InsuranceProduct insurance = InsuranceProduct.builder()
                .user(user)
                .resultId(request.getResultId())
                .insuranceCompany(nullToEmpty(result.getInsuranceCompany()))
                .productName(nullToEmpty(result.getProductName()))
                .isLongTerm(result.isLongTerm())
                .monthlyCost(result.getMonthlyCost() == null ? 0L : result.getMonthlyCost().longValue())
                .insuranceRecommendationReason(nullToEmpty(result.getInsuranceRecommendationReason()))
                .build();

        // 선택 특약만 저장
        Set<String> selected = new HashSet<>(request.getSelectedContractNames());

        if (result.getSpecialContracts() == null || result.getSpecialContracts().isEmpty()) {
            throw new CustomException(ExceptionCode.AI_PROCESSING_FAILED);
        }

        for (var sc : result.getSpecialContracts()) {
            if (sc == null || sc.getContractName() == null) continue;
            if (!selected.contains(sc.getContractName())) continue;

            String keyFeaturesJson = toJson(sc.getKeyFeatures());

            SpecialContract contract = SpecialContract.builder()
                    .contractName(sc.getContractName())
                    .contractDescription(nullToEmpty(sc.getContractDescription()))
                    .contractRecommendationReason(nullToEmpty(sc.getContractRecommendationReason()))
                    .keyFeatures(keyFeaturesJson == null ? "[]" : keyFeaturesJson)
                    .pageNumber(sc.getPageNumber() == null ? 0L : sc.getPageNumber().longValue())
                    .build();

            insurance.addContract(contract);
        }

        if (insurance.getSpecialContracts().isEmpty()) {
            throw new CustomException(ExceptionCode.AI_SAVE_EMPTY_SELECTION);
        }

        // 근거도 같이 저장
        if (result.getEvidenceSources() != null) {
            for (var ev : result.getEvidenceSources()) {
                if (ev == null) continue;

                EvidenceSource evidence = EvidenceSource.builder()
                        .pageNumber(ev.getPageNumber() == null ? 0L : ev.getPageNumber().longValue())
                        .textSnippet(nullToEmpty(ev.getTextSnippet()))
                        .build();

                insurance.addEvidence(evidence);
            }
        }

        InsuranceProduct saved = insuranceProductRepository.save(insurance);
        return saved.getInsuranceId();
    }


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
}
