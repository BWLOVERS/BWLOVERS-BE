package com.capstone.bwlovers.ai.recommendation.service;

import com.capstone.bwlovers.ai.common.cache.AiCacheKeys;
import com.capstone.bwlovers.ai.recommendation.dto.response.RecommendationListResponse;
import com.capstone.bwlovers.ai.recommendation.dto.response.RecommendationResponse;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.fasterxml.jackson.core.JsonProcessingException;
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
public class RecommendationCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void saveList(String resultId, RecommendationListResponse list, long ttlSec) {
        String key = AiCacheKeys.recommendListKey(resultId);
        try {
            String json = objectMapper.writeValueAsString(list);
            stringRedisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSec));
        } catch (RedisConnectionFailureException e) {
            throw new CustomException(ExceptionCode.REDIS_CONNECTION_FAILED);
        } catch (JsonProcessingException e) {
            throw new CustomException(ExceptionCode.JSON_SERIALIZATION_FAILED);
        } catch (DataAccessException e) {
            throw new CustomException(ExceptionCode.REDIS_SAVE_FAILED);
        }
    }

    public void saveDetail(String resultId, String itemId, RecommendationResponse detail, long ttlSec) {
        String key = AiCacheKeys.recommendDetailKey(resultId, itemId);
        try {
            String json = objectMapper.writeValueAsString(detail);
            stringRedisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSec));
        } catch (RedisConnectionFailureException e) {
            throw new CustomException(ExceptionCode.REDIS_CONNECTION_FAILED);
        } catch (JsonProcessingException e) {
            throw new CustomException(ExceptionCode.JSON_SERIALIZATION_FAILED);
        } catch (DataAccessException e) {
            throw new CustomException(ExceptionCode.REDIS_SAVE_FAILED);
        }
    }

    public RecommendationListResponse getList(String resultId) {
        String key = AiCacheKeys.recommendListKey(resultId);
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null) return null;
            return objectMapper.readValue(json, RecommendationListResponse.class);
        } catch (RedisConnectionFailureException e) {
            throw new CustomException(ExceptionCode.REDIS_CONNECTION_FAILED);
        } catch (JsonProcessingException e) {
            throw new CustomException(ExceptionCode.JSON_SERIALIZATION_FAILED);
        } catch (DataAccessException e) {
            throw new CustomException(ExceptionCode.REDIS_READ_FAILED);
        }
    }

    public RecommendationResponse getDetail(String resultId, String itemId) {
        String key = AiCacheKeys.recommendDetailKey(resultId, itemId);
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null) return null;
            return objectMapper.readValue(json, RecommendationResponse.class);
        } catch (RedisConnectionFailureException e) {
            throw new CustomException(ExceptionCode.REDIS_CONNECTION_FAILED);
        } catch (JsonProcessingException e) {
            throw new CustomException(ExceptionCode.JSON_SERIALIZATION_FAILED);
        } catch (DataAccessException e) {
            throw new CustomException(ExceptionCode.REDIS_READ_FAILED);
        }
    }

    public void saveListSafely(String resultId, RecommendationListResponse list, long ttlSec) {
        try {
            saveList(resultId, list, ttlSec);
        } catch (CustomException e) {
            log.warn("[RECOMMENDATION_CACHE_SAVE_BYPASS] resultId={}, ttlSec={}", resultId, ttlSec, e);
        }
    }

    public void saveDetailSafely(String resultId, String itemId, RecommendationResponse detail, long ttlSec) {
        try {
            saveDetail(resultId, itemId, detail, ttlSec);
        } catch (CustomException e) {
            log.warn(
                    "[RECOMMENDATION_CACHE_SAVE_BYPASS] resultId={}, itemId={}, ttlSec={}",
                    resultId,
                    itemId,
                    ttlSec,
                    e
            );
        }
    }

    public RecommendationListResponse findListSafely(String resultId) {
        try {
            return getList(resultId);
        } catch (CustomException e) {
            log.warn("[RECOMMENDATION_CACHE_READ_BYPASS] resultId={}", resultId, e);
            return null;
        }
    }

    public RecommendationResponse findDetailSafely(String resultId, String itemId) {
        try {
            return getDetail(resultId, itemId);
        } catch (CustomException e) {
            log.warn("[RECOMMENDATION_CACHE_READ_BYPASS] resultId={}, itemId={}", resultId, itemId, e);
            return null;
        }
    }
}
