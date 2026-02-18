package com.capstone.bwlovers.ai.recommendation.service;

import com.capstone.bwlovers.ai.common.cache.AiCacheKeys;
import com.capstone.bwlovers.ai.recommendation.dto.response.RecommendationListResponse;
import com.capstone.bwlovers.ai.recommendation.dto.response.RecommendationResponse;
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
public class RecommendationCacheService {

    private final StringRedisTemplate stringRedisTemplate;

    // JSON 직렬화/역직렬화는 ObjectMapper로 통일
    private final ObjectMapper objectMapper = new ObjectMapper();

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
}
