package com.capstone.bwlovers.ai.simulation.service;

import com.capstone.bwlovers.ai.simulation.dto.request.SimulationCallbackRequest;
import com.capstone.bwlovers.ai.simulation.dto.request.SimulationRequest;
import com.capstone.bwlovers.ai.simulation.dto.response.SimulationCreateResponse;
import com.capstone.bwlovers.ai.simulation.dto.response.SimulationResultResponse;
import com.capstone.bwlovers.auth.repository.UserRepository;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationService {

    private static final long DEFAULT_TTL_SEC = 600L;

    private final UserRepository userRepository;
    private final WebClient aiWebClient;
    private final SimulationCacheService simulationCacheService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 시뮬레이션 요청
     * - AI가 동기로 결과를 주면 즉시 Redis 저장함
     */
    public SimulationCreateResponse requestSimulation(Long userId, SimulationRequest req) {

        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        validateRequest(req);

        String raw = aiWebClient.post()
                .uri("/ai/simulation")
                .bodyValue(req)
                .retrieve()
                .onStatus(s -> s.value() == 400 || s.value() == 422,
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_INVALID_REQUEST)))
                .onStatus(s -> s.value() == 409,
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_PROCESSING_FAILED)))
                .onStatus(s -> s.is5xxServerError(),
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_SERVER_5XX)))
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(25))
                .block();

        log.info("[AI /ai/simulation RAW RESPONSE] {}", raw);

        if (isBlank(raw)) {
            throw new CustomException(ExceptionCode.AI_SERVER_5XX);
        }

        String simulationId = tryParseAndCacheIfPossible(raw);
        if (isBlank(simulationId)) {
            throw new CustomException(ExceptionCode.AI_SERVER_5XX);
        }

        return SimulationCreateResponse.of(simulationId);
    }

    /**
     * AI callback 결과를 Redis에 저장
     */
    public void cacheCallbackResult(SimulationCallbackRequest callback) {

        if (callback == null || isBlank(callback.getSimulationId())) {
            throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        }

        long ttlSec = (callback.getExpiresInSec() == null ? DEFAULT_TTL_SEC : callback.getExpiresInSec());

        SimulationResultResponse result = SimulationResultResponse.fromCallback(callback);
        simulationCacheService.saveResult(callback.getSimulationId(), result, ttlSec);

        log.info("[SIMULATION CALLBACK CACHED] simulationId={}, ttlSec={}", callback.getSimulationId(), ttlSec);
    }

    /**
     * Redis에서 시뮬레이션 결과 조회
     */
    public SimulationResultResponse getSimulationResult(String simulationId) {
        if (isBlank(simulationId)) {
            throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        }

        SimulationResultResponse cached = simulationCacheService.getResult(simulationId);
        if (cached == null) {
            throw new CustomException(ExceptionCode.AI_RESULT_NOT_FOUND);
        }
        return cached;
    }

    // =========================================================
    // private
    // =========================================================

    private void validateRequest(SimulationRequest req) {
        if (req == null) throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        if (isBlank(req.getInsuranceCompany())) throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        if (isBlank(req.getProductName())) throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        if (isBlank(req.getQuestion())) throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        if (req.getSpecialContracts() == null || req.getSpecialContracts().isEmpty()) {
            throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        }
    }

    /**
     * AI 응답 raw를 가능한 형태로 파싱함
     * - callback 형태(결과 포함)면 Redis에 저장까지 함
     * - 아니면 최소 simulationId만 뽑아서 반환함
     */
    private String tryParseAndCacheIfPossible(String raw) {
        // 1) 결과 포함 형태로 먼저 시도함
        try {
            SimulationCallbackRequest cb = objectMapper.readValue(raw, SimulationCallbackRequest.class);
            if (!isBlank(cb.getSimulationId())) {
                // result가 존재하면 캐시 저장함(동기 결과형)
                if (!isBlank(cb.getResult())) {
                    cacheCallbackResult(cb);
                }
                return cb.getSimulationId();
            }
        } catch (Exception ignore) {

        }

        // 2) 최소 {simulationId, expiresInSec} 같은 ack 형태를 가정하고 파싱
        try {
            SimulationAck ack = objectMapper.readValue(raw, SimulationAck.class);
            return ack.simulationId;
        } catch (Exception e) {
            log.error("[AI /ai/simulation PARSE ERROR] raw={}", raw, e);
            return null;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // AI가 결과가 아닌 ack만 주는 경우를 위한 내부 클래스
    private static class SimulationAck {
        public String simulationId;
        public Integer expiresInSec;
    }
}