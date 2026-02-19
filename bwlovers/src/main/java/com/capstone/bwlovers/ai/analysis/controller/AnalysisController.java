package com.capstone.bwlovers.ai.analysis.controller;

import com.capstone.bwlovers.ai.analysis.dto.request.AnalysisCallbackRequest;
import com.capstone.bwlovers.ai.analysis.dto.response.AnalysisCreateResponse;
import com.capstone.bwlovers.ai.analysis.dto.response.AnalysisResultResponse;
import com.capstone.bwlovers.ai.analysis.service.AnalysisService;
import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.ai.analysis.dto.request.SimulationRunRequest;
import jakarta.validation.Valid;
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
public class AnalysisController {

    private final AnalysisService analysisService;

    /**
     * 시뮬레이션 요청
     * POST /ai/simulation
     */
    @PostMapping("/simulation")
    public ResponseEntity<AnalysisCreateResponse> runSimulation(@AuthenticationPrincipal User user,
                                                                @Valid @RequestBody SimulationRunRequest request) {
        return ResponseEntity.ok(analysisService.requestSimulation(user.getUserId(), request));
    }

    /**
     * 시뮬레이션 결과 조회(저장 전)
     * GET /ai/simulation/{resultId}
     */
    @GetMapping("/simulation/{resultId}")
    public AnalysisResultResponse getResult(@AuthenticationPrincipal User user,
                                            @PathVariable String resultId) {
        return analysisService.getSimulationResult(resultId);
    }

    /**
     * AI 콜백 수신
     * POST /ai/callback/simulation
     */
    @PostMapping(path = "/callback/simulation", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receiveCallback(@RequestBody AnalysisCallbackRequest body) {
        analysisService.cacheCallbackResult(body);
        return ResponseEntity.ok().build();
    }
}