package com.capstone.bwlovers.ai.analysis.service;

import com.capstone.bwlovers.ai.analysis.dto.request.AnalysisCallbackRequest;
import com.capstone.bwlovers.ai.analysis.dto.request.AnalysisRequest;
import com.capstone.bwlovers.ai.analysis.dto.request.SimulationRunRequest;
import com.capstone.bwlovers.ai.analysis.dto.response.AnalysisCreateResponse;
import com.capstone.bwlovers.ai.analysis.dto.response.AnalysisResultResponse;
import com.capstone.bwlovers.auth.repository.UserRepository;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.capstone.bwlovers.insurance.domain.InsuranceProduct;
import com.capstone.bwlovers.insurance.domain.SpecialContract;
import com.capstone.bwlovers.insurance.repository.InsuranceProductRepository;
import com.capstone.bwlovers.insurance.repository.SpecialContractRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
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

    private final ObjectMapper objectMapper;

    public AnalysisCreateResponse requestSimulation(Long userId, SimulationRunRequest runReq) {

        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        validateRunRequest(runReq);

        InsuranceProduct insurance = insuranceProductRepository.findById(runReq.getInsuranceId())
                .orElseThrow(() -> new CustomException(ExceptionCode.INSURANCE_NOT_FOUND));

        if (!insurance.getUser().getUserId().equals(userId)) {
            throw new CustomException(ExceptionCode.USER_NOT_FOUND);
        }

        List<Long> contractIds = runReq.getSelectedContractIds();
        List<SpecialContract> selectedContracts =
                specialContractRepository.findAllByInsuranceProduct_InsuranceIdAndContractIdIn(
                        runReq.getInsuranceId(), contractIds
                );

        if (selectedContracts == null || selectedContracts.isEmpty()) {
            throw new CustomException(ExceptionCode.AI_SAVE_EMPTY_SELECTION);
        }

        if (selectedContracts.size() != contractIds.size()) {
            throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        }

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

        String resultId = tryParseAndCacheIfPossible(raw);
        if (isBlank(resultId)) {
            throw new CustomException(ExceptionCode.AI_SERVER_5XX);
        }

        return AnalysisCreateResponse.of(resultId);
    }

    public void cacheCallbackResult(AnalysisCallbackRequest callback) {

        if (callback == null || isBlank(callback.getResultId())) {
            throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        }

        long ttlSec = (callback.getExpiresInSec() == null ? DEFAULT_TTL_SEC : callback.getExpiresInSec());

        AnalysisResultResponse result = AnalysisResultResponse.fromCallback(callback);
        analysisCacheService.saveResult(callback.getResultId(), result, ttlSec);

        log.info("[SIMULATION CALLBACK CACHED] resultId={}, ttlSec={}", callback.getResultId(), ttlSec);
    }

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
        ObjectMapper lenient = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            AnalysisCallbackRequest cb = lenient.readValue(raw, AnalysisCallbackRequest.class);
            if (!isBlank(cb.getResultId())) {
                if (!isBlank(cb.getResult())) {
                    cacheCallbackResult(cb);
                }
                return cb.getResultId();
            }
        } catch (Exception ignore) {
            // fallback 시도함
        }

        try {
            SimulationAck ack = lenient.readValue(raw, SimulationAck.class);
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SimulationAck {
        public String resultId;
        public Integer expiresInSec;
    }
}