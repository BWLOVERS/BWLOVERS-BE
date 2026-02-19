package com.capstone.bwlovers.ai.analysis.service;

import com.capstone.bwlovers.ai.common.cache.AiCacheKeys;
import com.capstone.bwlovers.ai.analysis.dto.response.AnalysisResultResponse;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void saveResult(String resultId, AnalysisResultResponse result, long ttlSec) {
        String key = AiCacheKeys.simulationResultKey(resultId);
        try {
            String json = objectMapper.writeValueAsString(result);
            stringRedisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSec));
        } catch (RedisConnectionFailureException e) {
            log.warn("[REDIS_CONNECTION_FAILED] key={}", key, e);
            throw new CustomException(ExceptionCode.REDIS_CONNECTION_FAILED);
        } catch (JsonProcessingException e) {
            log.warn("[JSON_SERIALIZATION_FAILED][SAVE] key={}, type={}",
                    key, (result == null ? "null" : result.getClass().getName()), e);
            throw new CustomException(ExceptionCode.JSON_SERIALIZATION_FAILED);
        } catch (DataAccessException e) {
            log.warn("[REDIS_SAVE_FAILED] key={}", key, e);
            throw new CustomException(ExceptionCode.REDIS_SAVE_FAILED);
        }
    }

    public AnalysisResultResponse getResult(String resultId) {
        String key = AiCacheKeys.simulationResultKey(resultId);
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) return null;

            ObjectMapper lenient = objectMapper.copy()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            return lenient.readValue(json, AnalysisResultResponse.class);

        } catch (RedisConnectionFailureException e) {
            log.warn("[REDIS_CONNECTION_FAILED] key={}", key, e);
            throw new CustomException(ExceptionCode.REDIS_CONNECTION_FAILED);
        } catch (JsonProcessingException e) {
            String json = null;
            try {
                json = stringRedisTemplate.opsForValue().get(key);
            } catch (Exception ignore) {}

            log.warn("[JSON_SERIALIZATION_FAILED][READ] key={}, json={}", key, json, e);
            throw new CustomException(ExceptionCode.JSON_SERIALIZATION_FAILED);
        } catch (DataAccessException e) {
            log.warn("[REDIS_READ_FAILED] key={}", key, e);
            throw new CustomException(ExceptionCode.REDIS_READ_FAILED);
        }
    }

    public void delete(String resultId) {
        String key = AiCacheKeys.simulationResultKey(resultId);
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("[REDIS_DELETE_FAILED] key={}", key, e);
        }
    }
}