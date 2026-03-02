package com.capstone.bwlovers.ai.recommendation.service;

import com.capstone.bwlovers.ai.recommendation.dto.request.AiHealthStatus;
import com.capstone.bwlovers.ai.recommendation.dto.request.AiUserProfile;
import com.capstone.bwlovers.ai.recommendation.dto.request.RecommendationCallbackRequest;
import com.capstone.bwlovers.ai.recommendation.dto.request.RecommendationRequest;
import com.capstone.bwlovers.ai.recommendation.dto.response.FastApiResponse;
import com.capstone.bwlovers.ai.recommendation.dto.response.RecommendationListResponse;
import com.capstone.bwlovers.ai.recommendation.dto.response.RecommendationResponse;
import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.auth.repository.UserRepository;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.capstone.bwlovers.health.domain.HealthStatus;
import com.capstone.bwlovers.health.repository.HealthStatusRepository;
import com.capstone.bwlovers.insurance.repository.InsuranceProductRepository;
import com.capstone.bwlovers.pregnancy.domain.PregnancyInfo;
import com.capstone.bwlovers.pregnancy.repository.PregnancyInfoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static software.amazon.awssdk.utils.StringUtils.isBlank;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final long DEFAULT_TTL_SEC = 600L; // 10분
    private static final Duration AI_RECOMMEND_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration AI_DETAIL_TIMEOUT = Duration.ofSeconds(15);

    private final UserRepository userRepository;
    private final PregnancyInfoRepository pregnancyInfoRepository;
    private final HealthStatusRepository healthStatusRepository;

    private final WebClient aiWebClient;
    private final RecommendationCacheService recommendationCacheService;
    private final InsuranceProductRepository insuranceProductRepository;
    private final ObjectMapper objectMapper;

    /**
     * 결과 전체를 바로 받는 방식
     */
    public FastApiResponse requestAiRecommendation(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
        PregnancyInfo pregnancyInfo = pregnancyInfoRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ExceptionCode.PREGNANCY_INFO_NOT_FOUND));
        HealthStatus healthStatus = healthStatusRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ExceptionCode.HEALTH_STATUS_NOT_FOUND));

        RecommendationRequest dto = toFastApiRequest(pregnancyInfo, healthStatus);

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
                .timeout(AI_RECOMMEND_TIMEOUT)
                .doOnSubscribe(s -> log.info("[AI] POST /ai/recommend start userId={}", userId))
                .doOnSuccess(r -> log.info("[AI] POST /ai/recommend success userId={} resultId={}",
                        userId, r == null ? null : r.getResultId()))
                .doOnError(e -> log.error("[AI] POST /ai/recommend error userId={}", userId, e))
                .block();
    }

    /**
     * 추천 리스트 조회
     * - FastAPI 응답(list)을 Redis에 저장
     */
    public RecommendationListResponse requestAiRecommendationList(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
        PregnancyInfo pregnancyInfo = pregnancyInfoRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ExceptionCode.PREGNANCY_INFO_NOT_FOUND));
        HealthStatus healthStatus = healthStatusRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ExceptionCode.HEALTH_STATUS_NOT_FOUND));

        RecommendationRequest dto = toFastApiRequest(pregnancyInfo, healthStatus);

        String raw = aiWebClient.post()
                .uri("/ai/recommend")
                .bodyValue(dto)
                .retrieve()
                .onStatus(s -> s.value() == 400 || s.value() == 422,
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_INVALID_REQUEST)))
                .onStatus(s -> s.value() == 409,
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_PROCESSING_FAILED)))
                .onStatus(s -> s.is5xxServerError(),
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_SERVER_5XX)))
                .bodyToMono(String.class)
                .timeout(AI_RECOMMEND_TIMEOUT)
                .doOnSubscribe(s -> log.info("[AI] POST /ai/recommend(list) start userId={}", userId))
                .doOnError(e -> log.error("[AI] POST /ai/recommend(list) error userId={}", userId, e))
                .block();

        log.info("[AI /ai/recommend RAW RESPONSE] {}", raw);

        RecommendationListResponse list;
        try {
            list = objectMapper.readValue(raw, RecommendationListResponse.class);
        } catch (Exception e) {
            log.error("[AI /ai/recommend PARSE ERROR] raw={}", raw, e);
            throw new CustomException(ExceptionCode.AI_SERVER_5XX);
        }

        if (list == null || isBlank(list.getResultId())) {
            log.warn("[AI /ai/recommend INVALID] resultId is blank. raw={}", raw);
            throw new CustomException(ExceptionCode.AI_SERVER_5XX);
        }

        list.normalizeAllCounts();

        long ttlSec = (list.getExpiresInSec() == null ? DEFAULT_TTL_SEC : list.getExpiresInSec());

        recommendationCacheService.saveList(list.getResultId(), list, ttlSec);

        if (list.getItems() != null) {
            for (var item : list.getItems()) {
                if (item == null || isBlank(item.getItemId())) continue;

                RecommendationResponse detail = RecommendationResponse.fromListItem(item);
                recommendationCacheService.saveDetail(list.getResultId(), item.getItemId(), detail, ttlSec);
            }
        }

        return list;
    }

    /**
     * 결과 상세 보기
     * GET /ai/results/{resultId}/items/{itemId}
     */
    public RecommendationResponse fetchAiResultDetail(Long userId, String resultId, String itemId) {

        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        if (isBlank(resultId) || isBlank(itemId)) {
            throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        }

        RecommendationResponse cached = recommendationCacheService.getDetail(resultId, itemId);
        if (cached != null) {
            log.info("[FETCH DETAIL] Redis cache HIT. resultId={} itemId={}", resultId, itemId);
            return cached;
        }

        RecommendationResponse fresh = aiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ai/results/{resultId}/items/{itemId}")
                        .build(resultId, itemId))
                .retrieve()
                .onStatus(s -> s.is4xxClientError(),
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_RESULT_NOT_FOUND)))
                .onStatus(s -> s.is5xxServerError(),
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_SERVER_5XX)))
                .bodyToMono(RecommendationResponse.class)
                .timeout(AI_DETAIL_TIMEOUT)
                .doOnSubscribe(s -> log.info("[AI] GET /ai/results/{}/items/{} start", resultId, itemId))
                .doOnError(e -> log.error("[AI] GET /ai/results/{}/items/{} error", resultId, itemId, e))
                .block();

        if (fresh != null) {
            recommendationCacheService.saveDetail(resultId, itemId, fresh, DEFAULT_TTL_SEC);
        }

        return fresh;
    }

    /**
     * AI callback 결과를 Redis에 저장함
     */
    public void cacheCallbackResult(RecommendationCallbackRequest callback) {

        if (callback == null || isBlank(callback.getResultId())) {
            throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        }

        long ttlSec = (callback.getExpiresInSec() == null ? DEFAULT_TTL_SEC : callback.getExpiresInSec());

        RecommendationListResponse list = RecommendationListResponse.fromCallback(callback);
        recommendationCacheService.saveList(callback.getResultId(), list, ttlSec);

        if (callback.getItems() != null) {
            for (var item : callback.getItems()) {
                if (item == null || isBlank(item.getItemId())) continue;

                RecommendationResponse detail = RecommendationResponse.fromCallbackItem(item);
                recommendationCacheService.saveDetail(callback.getResultId(), item.getItemId(), detail, ttlSec);
            }
        }

        log.info("[AI CALLBACK CACHED] resultId={}, ttlSec={}, items={}",
                callback.getResultId(),
                ttlSec,
                callback.getItems() == null ? 0 : callback.getItems().size());
    }

    // =========================================================
    // private
    // =========================================================

    //원하는 JSON 형태로 변환
    private RecommendationRequest toFastApiRequest(PregnancyInfo pregnancyInfo, HealthStatus healthStatus) {

        String jobName = (pregnancyInfo.getJob() == null) ? null : pregnancyInfo.getJob().getJobName();
        Integer riskLevel = (pregnancyInfo.getJob() == null) ? null : pregnancyInfo.getJob().getRiskLevel();

        AiUserProfile p = AiUserProfile.builder()
                .birthDate(pregnancyInfo.getBirthDate() == null ? null : pregnancyInfo.getBirthDate().toString())
                .height(pregnancyInfo.getHeight())
                .weightPre(pregnancyInfo.getWeightPre())
                .weightCurrent(pregnancyInfo.getWeightCurrent())
                .isFirstbirth(pregnancyInfo.getIsFirstbirth())
                .gestationalWeek(pregnancyInfo.getGestationalWeek())
                .expectedDate(pregnancyInfo.getExpectedDate() == null ? null : pregnancyInfo.getExpectedDate().toString())
                .isMultiplePregnancy(pregnancyInfo.getIsMultiplePregnancy())
                .miscarriageHistory(pregnancyInfo.getMiscarriageHistory())
                .jobName(jobName)
                .riskLevel(riskLevel)
                .build();

        AiHealthStatus h = AiHealthStatus.builder()
                .pastDiseases(
                        healthStatus.getPastDiseases().stream()
                                .map(x -> AiHealthStatus.AiPastDisease.builder()
                                        .pastDiseaseType(x.getPastDiseaseType().name())
                                        .pastCured(x.isPastCured())
                                        .pastLastTreatedYm(x.getPastLastTreatedAt() == null ? null : x.getPastLastTreatedAt().toString().substring(0, 7))
                                        .build())
                                .toList()
                )
                .chronicDiseases(
                        healthStatus.getChronicDiseases().stream()
                                .map(x -> AiHealthStatus.AiChronicDisease.builder()
                                        .chronicDiseaseType(x.getChronicDiseaseType().name())
                                        .chronicOnMedication(x.isChronicOnMedication())
                                        .build())
                                .toList()
                )
                .pregnancyComplications(
                        healthStatus.getPregnancyComplications().stream()
                                .map(pc -> AiHealthStatus.AiPregnancyComplication.builder()
                                        .complicationId(pc.getComplicationId())
                                        .pregnancyComplicationType(pc.getPregnancyComplicationType().name())
                                        .build())
                                .toList()
                )
                .build();

        return RecommendationRequest.builder()
                .pregnancyInfo(p)
                .healthStatus(h)
                .build();
    }
}