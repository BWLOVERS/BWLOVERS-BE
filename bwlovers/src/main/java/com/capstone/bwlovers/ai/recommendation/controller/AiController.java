package com.capstone.bwlovers.ai.recommendation.controller;

import com.capstone.bwlovers.ai.recommendation.dto.request.AiCallbackRequest;
import com.capstone.bwlovers.ai.recommendation.dto.response.AiRecommendationListResponse;
import com.capstone.bwlovers.ai.recommendation.dto.response.AiRecommendationResponse;
import com.capstone.bwlovers.ai.recommendation.service.AiResultCacheService;
import com.capstone.bwlovers.ai.recommendation.service.AiService;
import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;
    private final AiResultCacheService aiResultCacheService;
    private final ObjectMapper objectMapper;

    /**
     * 추천 요청 POST /ai/recommend
     * - 기존 그대로: FastAPI 호출해서 resultId 포함된 리스트 받음
     */
    @PostMapping("/recommend")
    public AiRecommendationListResponse recommendList(@AuthenticationPrincipal User user) {
        return aiService.requestAiRecommendationList(user.getUserId());
    }

    /**
     * 보험 추천 리스트 조회(저장 전) POST /ai/recommend/{resultId}
     * - Redis에서 리스트 형태로 꺼내줌
     */
    @GetMapping("/recommend/{resultId}")
    public AiRecommendationListResponse getListFromRedis(@AuthenticationPrincipal User user,
                                                         @PathVariable String resultId) {
        AiRecommendationListResponse cached = aiResultCacheService.getList(resultId);
        if (cached == null) {
            throw new CustomException(ExceptionCode.AI_RESULT_NOT_FOUND);
        }
        return cached;
    }

    /**
     * 보험 추천 상세 조회 GET /ai/results/{resultId}/items/{itemId}
     * - Redis에서 상세 꺼내줌 (없으면 기존 FastAPI 조회로 fallback 하고 싶으면 AiService에서 처리 가능함)
     */
    @GetMapping("/results/{resultId}/items/{itemId}")
    public AiRecommendationResponse getDetail(@AuthenticationPrincipal User user,
                                              @PathVariable String resultId,
                                              @PathVariable String itemId) {

        AiRecommendationResponse cached = aiResultCacheService.getDetail(resultId, itemId);
        if (cached != null) return cached;

        // fallback(선택): 기존 FastAPI 조회 유지
        return aiService.fetchAiResultDetail(user.getUserId(), resultId, itemId);
    }

    /**
     * 요청 콜백 POST /ai/callback/recommend
     * - 여기서 Redis에 "리스트/상세" 둘 다 저장함
     */
    @PostMapping(path="/callback/recommend", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receive(@RequestBody AiCallbackRequest body) {
        aiService.cacheCallbackResult(body);
        return ResponseEntity.ok().build();
    }

}
