package com.capstone.bwlovers.ai.ocr.infra.cache;

import com.capstone.bwlovers.ai.common.cache.AiCacheKeys;
import com.capstone.bwlovers.ai.ocr.domain.OcrJobCache;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OcrJobCacheRepository {

    @Qualifier("ocrJobCacheRedisTemplate")
    private final RedisTemplate<String, OcrJobCache> redisTemplate;

    @Value("${analysis.ocr.job-ttl-seconds:1800}")
    private long ttlSeconds;

    private String key(String jobId) {
        return AiCacheKeys.OCR_JOB_PREFIX + jobId;
    }

    public void save(OcrJobCache cache) {
        try {
            redisTemplate.opsForValue().set(key(cache.getJobId()), cache, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            throw new CustomException(ExceptionCode.REDIS_SAVE_FAILED);
        }
    }

    public Optional<OcrJobCache> find(String jobId) {
        try {
            OcrJobCache v = redisTemplate.opsForValue().get(key(jobId));
            return Optional.ofNullable(v);
        } catch (Exception e) {
            throw new CustomException(ExceptionCode.REDIS_READ_FAILED);
        }
    }

    public void delete(String jobId) {
        redisTemplate.delete(key(jobId));
    }
}