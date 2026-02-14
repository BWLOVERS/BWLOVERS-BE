package com.capstone.bwlovers.ai.ocr.infra.cache;

import com.capstone.bwlovers.ai.ocr.domain.OcrJobCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

@Configuration
@RequiredArgsConstructor
public class OcrRedisConfig {

    @Bean(name = "ocrJobCacheRedisTemplate")
    public RedisTemplate<String, OcrJobCache> ocrJobCacheRedisTemplate(
            RedisConnectionFactory cf
    ) {
        RedisTemplate<String, OcrJobCache> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);

        // key: string
        template.setKeySerializer(new StringRedisSerializer());

        // value: JSON -> OcrJobCache로 역직렬화되게 세팅
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());

        Jackson2JsonRedisSerializer<OcrJobCache> valueSerializer =
                new Jackson2JsonRedisSerializer<>(om, OcrJobCache.class);

        template.setValueSerializer(valueSerializer);

        // hash도 쓸 수 있으니 맞춰둠
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }
}