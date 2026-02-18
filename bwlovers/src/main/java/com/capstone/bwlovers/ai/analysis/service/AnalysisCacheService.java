package com.capstone.bwlovers.ai.analysis.service;

import com.capstone.bwlovers.ai.common.cache.AiCacheKeys;
import com.capstone.bwlovers.ai.analysis.dto.response.AnalysisResultResponse;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AnalysisCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void saveResult(String resultId, AnalysisResultResponse result, long ttlSec) {
        String key = AiCacheKeys.simulationResultKey(resultId);
        try {
            String json = objectMapper.writeValueAsString(result);
            stringRedisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSec));
        } catch (RedisConnectionFailureException e) {
            throw new CustomException(ExceptionCode.REDIS_CONNECTION_FAILED);
        } catch (JsonProcessingException e) {
            throw new CustomException(ExceptionCode.JSON_SERIALIZATION_FAILED);
        } catch (DataAccessException e) {
            throw new CustomException(ExceptionCode.REDIS_SAVE_FAILED);
        }
    }

    public AnalysisResultResponse getResult(String resultId) {
        String key = AiCacheKeys.simulationResultKey(resultId);
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null) return null;
            return objectMapper.readValue(json, AnalysisResultResponse.class);
        } catch (RedisConnectionFailureException e) {
            throw new CustomException(ExceptionCode.REDIS_CONNECTION_FAILED);
        } catch (JsonProcessingException e) {
            throw new CustomException(ExceptionCode.JSON_SERIALIZATION_FAILED);
        } catch (DataAccessException e) {
            throw new CustomException(ExceptionCode.REDIS_READ_FAILED);
        }
    }

    public void delete(String resultId) {
        stringRedisTemplate.delete(AiCacheKeys.simulationResultKey(resultId));
    }
}