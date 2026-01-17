//package com.capstone.bwlovers.ai.controller;
//
//import com.capstone.bwlovers.ai.dto.FastApiResponse;
//import com.capstone.bwlovers.ai.service.AiService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/ai")
//public class AiController {
//
//    private final AiService aiService;
//
//    @GetMapping("/recommend/{userId}")
//    public FastApiResponse recommend(@PathVariable Long userId) {
//        return aiService.requestAiRecommendation(userId);
//    }
//}
//
