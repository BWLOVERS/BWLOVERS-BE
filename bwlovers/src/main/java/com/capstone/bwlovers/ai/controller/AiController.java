package com.capstone.bwlovers.ai.controller;

import com.capstone.bwlovers.ai.dto.request.AiSaveSelectedRequest;
import com.capstone.bwlovers.ai.dto.response.AiRecommendationListResponse;
import com.capstone.bwlovers.ai.dto.response.AiRecommendationResponse;
import com.capstone.bwlovers.ai.service.AiService;
import com.capstone.bwlovers.auth.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;

//    /**
//     * - FastAPI 결과 전체를 바로 반환하는 방식(보류)
//     */
//    @PostMapping("/recommend")
//    public FastApiResponse recommend(@AuthenticationPrincipal User user) {
//        return aiService.requestAiRecommendation(user.getUserId());
//    }

    /**
     * 추천 리스트 조회 POST /ai/recommend
     */
    @PostMapping("/recommend")
    public AiRecommendationListResponse recommendList(@AuthenticationPrincipal User user) {
        return aiService.requestAiRecommendationList(user.getUserId());
    }

    /**
     * 추천 리스트 상세 조회 GET /ai/results/{resultId}/items/{itemId}
     */
    @GetMapping("/results/{resultId}/items/{itemId}")
    public AiRecommendationResponse getDetail(@AuthenticationPrincipal User user,
                                              @PathVariable String resultId,
                                              @PathVariable String itemId) {
        return aiService.fetchAiResultDetail(user.getUserId(), resultId, itemId);
    }

    /**
     * 선택 저장 POST /ai/save
     */
    @PostMapping("/save")
    public Long saveSelected(@AuthenticationPrincipal User user,
                             @RequestBody AiSaveSelectedRequest request) {
        return aiService.saveSelected(user.getUserId(), request);
    }
}
