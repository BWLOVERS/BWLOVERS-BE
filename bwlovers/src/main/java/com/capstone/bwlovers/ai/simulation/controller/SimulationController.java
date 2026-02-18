package com.capstone.bwlovers.ai.simulation.controller;

import com.capstone.bwlovers.ai.simulation.dto.request.SimulationCallbackRequest;
import com.capstone.bwlovers.ai.simulation.dto.request.SimulationRequest;
import com.capstone.bwlovers.ai.simulation.dto.response.SimulationCreateResponse;
import com.capstone.bwlovers.ai.simulation.dto.response.SimulationResultResponse;
import com.capstone.bwlovers.ai.simulation.service.SimulationService;
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
public class SimulationController {

    private final SimulationService simulationService;

    /**
     * 시뮬레이션 요청
     * POST /ai/simulation
     */
    @PostMapping("/simulation")
    public SimulationCreateResponse requestSimulation(@AuthenticationPrincipal User user,
                                                      @RequestBody SimulationRequest request) {
        return simulationService.requestSimulation(user.getUserId(), request);
    }

    /**
     * 시뮬레이션 결과 조회(저장 전)
     * GET /ai/simulation/{simulationId}
     */
    @GetMapping("/simulation/{simulationId}")
    public SimulationResultResponse getResult(@AuthenticationPrincipal User user,
                                              @PathVariable String simulationId) {
        return simulationService.getSimulationResult(simulationId);
    }

    /**
     * AI 콜백 수신
     * POST /ai/callback/simulation
     */
    @PostMapping(path = "/callback/simulation", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receiveCallback(@RequestBody SimulationCallbackRequest body) {
        simulationService.cacheCallbackResult(body);
        return ResponseEntity.ok().build();
    }
}