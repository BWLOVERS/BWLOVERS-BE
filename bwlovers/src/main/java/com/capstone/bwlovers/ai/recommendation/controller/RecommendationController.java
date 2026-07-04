package com.capstone.bwlovers.ai.recommendation.controller;

import com.capstone.bwlovers.ai.recommendation.dto.request.RecommendationCallbackRequest;
import com.capstone.bwlovers.ai.recommendation.dto.response.RecommendationListResponse;
import com.capstone.bwlovers.ai.recommendation.dto.response.RecommendationResponse;
import com.capstone.bwlovers.ai.recommendation.service.RecommendationService;
import com.capstone.bwlovers.auth.domain.User;
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
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * 추천 요청 POST /ai/recommend
     */
    @PostMapping("/recommend")
    public RecommendationListResponse recommendList(@AuthenticationPrincipal User user) {
        return recommendationService.requestAiRecommendationList(user.getUserId());
    }

    /**
     * 보험 추천 리스트 조회 GET /ai/recommend/{resultId}
     */
    @GetMapping("/recommend/{resultId}")
    public RecommendationListResponse getListFromRedis(@AuthenticationPrincipal User user,
                                                       @PathVariable String resultId) {
        return recommendationService.getRecommendationList(resultId);
    }

    /**
     * 보험 추천 상세 조회 GET /ai/results/{resultId}/items/{itemId}
     */
    @GetMapping("/results/{resultId}/items/{itemId}")
    public RecommendationResponse getDetail(@AuthenticationPrincipal User user,
                                            @PathVariable String resultId,
                                            @PathVariable String itemId) {
        return recommendationService.fetchAiResultDetail(user.getUserId(), resultId, itemId);
    }

    /**
     * 추천 결과 콜백 POST /ai/callback/recommend
     */
    @PostMapping(path = "/callback/recommend", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receive(@RequestBody RecommendationCallbackRequest body) {
        recommendationService.cacheCallbackResult(body);
        return ResponseEntity.ok().build();
    }
}
