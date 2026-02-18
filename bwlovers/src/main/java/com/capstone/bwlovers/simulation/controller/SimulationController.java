package com.capstone.bwlovers.simulation.controller;

import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.simulation.dto.request.SimulationSaveRequest;
import com.capstone.bwlovers.simulation.dto.response.SimulationSaveResponse;
import com.capstone.bwlovers.simulation.service.SimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/simulations")
public class SimulationController {

    private final SimulationService simulationService;

    /**
     * 시뮬레이션 결과 저장
     * POST /simulations/save
     */
    @PostMapping("/save")
    public ResponseEntity<SimulationSaveResponse> save(@AuthenticationPrincipal User user,
                                                       @Valid @RequestBody SimulationSaveRequest request) {
        Long savedId = simulationService.saveSimulationResult(user.getUserId(), request);
        return ResponseEntity.ok(SimulationSaveResponse.of(savedId, request.getResultId()));
    }
}