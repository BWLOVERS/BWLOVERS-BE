package com.capstone.bwlovers.simulation.controller;

import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.simulation.dto.request.SimulationSaveRequest;
import com.capstone.bwlovers.simulation.dto.response.SimulationDetailListResponse;
import com.capstone.bwlovers.simulation.dto.response.SimulationDetailResponse;
import com.capstone.bwlovers.simulation.dto.response.SimulationListResponse;
import com.capstone.bwlovers.simulation.dto.response.SimulationSaveResponse;
import com.capstone.bwlovers.simulation.service.SimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * 시뮬레이션 리스트 조회
     * GET /simulations
     */
    @GetMapping
    public ResponseEntity<List<SimulationListResponse>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(simulationService.getSimulationList(user.getUserId()));
    }

    /**
     * 시뮬레이션 상세 리스트 조회
     * GET /simulations/details
     */
    @GetMapping("/details")
    public ResponseEntity<List<SimulationDetailListResponse>> detailList(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(simulationService.getSimulationDetailList(user.getUserId()));
    }

    /**
     * 시뮬레이션 상세보기
     * GET /simulations/{simulationId}
     */
    @GetMapping("/{simulationId}")
    public ResponseEntity<SimulationDetailResponse> detail(@AuthenticationPrincipal User user,
                                                           @PathVariable Long simulationId) {
        return ResponseEntity.ok(simulationService.getSimulationDetail(user.getUserId(), simulationId));
    }

    /**
     * 시뮬레이션 삭제
     * DELETE /simulations/{simulationId}
     */
    @DeleteMapping("/{simulationId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user,
                                       @PathVariable Long simulationId) {
        simulationService.deleteSimulation(user.getUserId(), simulationId);
        return ResponseEntity.noContent().build();
    }
}