package com.capstone.bwlovers.ai.recommendation.service;

import com.capstone.bwlovers.ai.recommendation.dto.response.RecommendationResponse;
import com.capstone.bwlovers.auth.domain.OAuthProvider;
import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.auth.repository.UserRepository;
import com.capstone.bwlovers.health.repository.HealthStatusRepository;
import com.capstone.bwlovers.pregnancy.repository.PregnancyInfoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PregnancyInfoRepository pregnancyInfoRepository;
    @Mock
    private HealthStatusRepository healthStatusRepository;
    @Mock
    private RecommendationCacheService recommendationCacheService;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        ExchangeFunction exchangeFunction = request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body("""
                                {
                                  "itemId": "item-1",
                                  "insurance_company": "ACME",
                                  "is_long_term": true,
                                  "product_name": "Safe Plan",
                                  "insurance_recommendation_reason": "추천 사유",
                                  "sum_insured": "1000",
                                  "monthly_cost": "10",
                                  "special_contracts": [],
                                  "evidence_sources": []
                                }
                                """)
                        .build()
        );

        recommendationService = new RecommendationService(
                userRepository,
                pregnancyInfoRepository,
                healthStatusRepository,
                WebClient.builder().exchangeFunction(exchangeFunction).build(),
                recommendationCacheService,
                new ObjectMapper()
        );
    }

    @Test
    void fetchAiResultDetail_readsFromAiWhenCacheMisses() {
        Long userId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(
                User.builder()
                        .userId(userId)
                        .provider(OAuthProvider.NAVER)
                        .providerId("provider-1")
                        .build()
        ));
        when(recommendationCacheService.findDetailSafely("result-1", "item-1")).thenReturn(null);

        RecommendationResponse response = recommendationService.fetchAiResultDetail(userId, "result-1", "item-1");

        assertEquals("item-1", response.getItemId());
        assertEquals("ACME", response.getInsuranceCompany());
        assertEquals("Safe Plan", response.getProductName());
        assertEquals("1000", response.getSumInsured());
        verify(recommendationCacheService)
                .saveDetailSafely(eq("result-1"), eq("item-1"), any(RecommendationResponse.class), eq(600L));
    }
}
