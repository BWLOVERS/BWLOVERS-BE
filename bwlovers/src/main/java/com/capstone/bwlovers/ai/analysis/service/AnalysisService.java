package com.capstone.bwlovers.ai.analysis.service;

import com.capstone.bwlovers.ai.analysis.dto.request.AnalysisCallbackRequest;
import com.capstone.bwlovers.ai.analysis.dto.request.AnalysisRequest;
import com.capstone.bwlovers.ai.analysis.dto.response.AnalysisCreateResponse;
import com.capstone.bwlovers.ai.analysis.dto.response.AnalysisResultResponse;
import com.capstone.bwlovers.auth.repository.UserRepository;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.capstone.bwlovers.insurance.domain.InsuranceProduct;
import com.capstone.bwlovers.insurance.domain.SpecialContract;
import com.capstone.bwlovers.insurance.repository.InsuranceProductRepository;
import com.capstone.bwlovers.insurance.repository.SpecialContractRepository;
import com.capstone.bwlovers.ai.analysis.dto.request.SimulationRunRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private static final long DEFAULT_TTL_SEC = 600L;

    private final UserRepository userRepository;
    private final WebClient aiWebClient;
    private final AnalysisCacheService analysisCacheService;

    private final InsuranceProductRepository insuranceProductRepository;
    private final SpecialContractRepository specialContractRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 시뮬레이션 요청 (ID 기반)
     * - 프론트는 insuranceId, selectedContractIds, question만 보냄
     * - 백엔드가 DB에서 보험/특약(pageNumber 포함)을 가져와 AI 요청(AnalysisRequest)로 변환함
     * - AI가 동기로 결과를 주면 즉시 Redis 저장함
     */
    public AnalysisCreateResponse requestSimulation(Long userId, SimulationRunRequest runReq) {

        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        validateRunRequest(runReq);

        // 1) 보험 조회 + 본인 소유 검증
        InsuranceProduct insurance = insuranceProductRepository.findById(runReq.getInsuranceId())
                .orElseThrow(() -> new CustomException(ExceptionCode.INSURANCE_NOT_FOUND));

        if (!insurance.getUser().getUserId().equals(userId)) {
            throw new CustomException(ExceptionCode.USER_NOT_FOUND);
        }

        // 2) 선택된 특약 조회(보험 범위로 묶어서 안전하게)
        List<Long> contractIds = runReq.getSelectedContractIds();
        List<SpecialContract> selectedContracts = specialContractRepository.findAllByInsuranceProduct_InsuranceIdAndContractIdIn(runReq.getInsuranceId(), contractIds);

        if (selectedContracts == null || selectedContracts.isEmpty()) {
            throw new CustomException(ExceptionCode.AI_SAVE_EMPTY_SELECTION);
        }

        // 보안/정합성 체크: 요청한 개수와 조회된 개수가 다르면 잘못된 contractId 포함임
        if (selectedContracts.size() != contractIds.size()) {
            throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        }

        // 3) AI 요청 DTO(AnalysisRequest) 조립: contract_name + page_number 자동 포함
        List<AnalysisRequest.SpecialContract> aiContracts = selectedContracts.stream()
                .map(sc -> AnalysisRequest.SpecialContract.builder()
                        .contractName(sc.getContractName())
                        .pageNumber(sc.getPageNumber() == null ? 0 : sc.getPageNumber().intValue())
                        .build())
                .toList();

        AnalysisRequest aiReq = AnalysisRequest.builder()
                .insuranceCompany(nullToEmpty(insurance.getInsuranceCompany()))
                .productName(nullToEmpty(insurance.getProductName()))
                .specialContracts(aiContracts)
                .question(runReq.getQuestion())
                .build();

        // 4) AI 호출
        String raw = aiWebClient.post()
                .uri("/ai/simulation")
                .bodyValue(aiReq)
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

        return AnalysisCreateResponse.of(simulationId);
    }

    /**
     * AI callback 결과를 Redis에 저장
     */
    public void cacheCallbackResult(AnalysisCallbackRequest callback) {

        if (callback == null || isBlank(callback.getResultId())) {
            throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        }

        long ttlSec = (callback.getExpiresInSec() == null ? DEFAULT_TTL_SEC : callback.getExpiresInSec());

        AnalysisResultResponse result = AnalysisResultResponse.fromCallback(callback);
        analysisCacheService.saveResult(callback.getResultId(), result, ttlSec);

        log.info("[SIMULATION CALLBACK CACHED] simulationId={}, ttlSec={}", callback.getResultId(), ttlSec);
    }

    /**
     * Redis에서 시뮬레이션 결과 조회
     */
    public AnalysisResultResponse getSimulationResult(String resultId) {
        if (isBlank(resultId)) {
            throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        }

        AnalysisResultResponse cached = analysisCacheService.getResult(resultId);
        if (cached == null) {
            throw new CustomException(ExceptionCode.AI_RESULT_NOT_FOUND);
        }
        return cached;
    }

    // =========================================================
    // private
    // =========================================================

    private void validateRunRequest(SimulationRunRequest req) {
        if (req == null) throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        if (req.getInsuranceId() == null) throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        if (isBlank(req.getQuestion())) throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        if (req.getSelectedContractIds() == null || req.getSelectedContractIds().isEmpty()) {
            throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        }
    }

    private String tryParseAndCacheIfPossible(String raw) {
        try {
            AnalysisCallbackRequest cb = objectMapper.readValue(raw, AnalysisCallbackRequest.class);
            if (!isBlank(cb.getResultId())) {
                if (!isBlank(cb.getResult())) {
                    cacheCallbackResult(cb);
                }
                return cb.getResultId();
            }
        } catch (Exception ignore) {

        }

        try {
            SimulationAck ack = objectMapper.readValue(raw, SimulationAck.class);
            return ack.resultId;
        } catch (Exception e) {
            log.error("[AI /ai/simulation PARSE ERROR] raw={}", raw, e);
            return null;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static class SimulationAck {
        public String resultId;
        public Integer expiresInSec;
    }
}