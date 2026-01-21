package com.capstone.bwlovers.ai.controller;

import com.capstone.bwlovers.ai.dto.response.FastApiResponse;
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

    @PostMapping("/recommend")
    public FastApiResponse recommend(@AuthenticationPrincipal User user) {
        return aiService.requestAiRecommendation(user.getUserId());
    }

}

