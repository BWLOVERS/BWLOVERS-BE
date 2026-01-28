package com.capstone.bwlovers.ai.controller;

import com.capstone.bwlovers.ai.dto.request.AiSaveSelectedRequest;
import com.capstone.bwlovers.ai.dto.response.AiRecommendTicketResponse;
import com.capstone.bwlovers.ai.dto.response.InsuranceRecommendationResponse;
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
     * 추천 요청 → resultId(ticket) 반환
     * 프론트는 이 resultId로 결과 조회함
     */
    @PostMapping("/recommend")
    public AiRecommendTicketResponse recommendTicket(@AuthenticationPrincipal User user) {
        return aiService.requestAiRecommendationTicket(user.getUserId());
    }

    /**
     * resultId로 결과 조회 (미리보기)
     */
    @GetMapping("/results/{resultId}")
    public InsuranceRecommendationResponse getResult(@AuthenticationPrincipal User user,
                                                     @PathVariable String resultId) {
        return aiService.fetchAiResult(user.getUserId(), resultId);
    }

    /**
     * 사용자가 선택한 특약만 저장
     */
    @PostMapping("/save")
    public Long saveSelected(@AuthenticationPrincipal User user,
                             @RequestBody AiSaveSelectedRequest request) {
        return aiService.saveSelected(user.getUserId(), request);
    }
}
