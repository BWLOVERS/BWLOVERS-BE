package com.capstone.bwlovers.ai.service;

import com.capstone.bwlovers.ai.cache.AiCacheKeys;
import com.capstone.bwlovers.ai.dto.response.AiRecommendationListResponse;
import com.capstone.bwlovers.ai.dto.response.AiRecommendationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AiResultCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void saveList(String resultId, AiRecommendationListResponse list, long ttlSec) {
        redisTemplate.opsForValue().set(AiCacheKeys.listKey(resultId), list, Duration.ofSeconds(ttlSec));
    }

    public void saveDetail(String resultId, String itemId, AiRecommendationResponse detail, long ttlSec) {
        redisTemplate.opsForValue().set(AiCacheKeys.detailKey(resultId, itemId), detail, Duration.ofSeconds(ttlSec));
    }

    public AiRecommendationListResponse getList(String resultId) {
        Object v = redisTemplate.opsForValue().get(AiCacheKeys.listKey(resultId));
        return (v == null) ? null : (AiRecommendationListResponse) v;
    }

    public AiRecommendationResponse getDetail(String resultId, String itemId) {
        Object v = redisTemplate.opsForValue().get(AiCacheKeys.detailKey(resultId, itemId));
        return (v == null) ? null : (AiRecommendationResponse) v;
    }


}
