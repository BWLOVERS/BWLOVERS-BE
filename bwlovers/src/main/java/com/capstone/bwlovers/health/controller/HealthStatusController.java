package com.capstone.bwlovers.health.controller;

import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.health.dto.request.HealthStatusRequest;
import com.capstone.bwlovers.health.dto.response.HealthStatusResponse;
import com.capstone.bwlovers.health.service.HealthStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/me/health-status")
public class HealthStatusController {

    private final HealthStatusService healthStatusService;

    /*
     POST /users/me/health-status : 산모 건강 상태 등록
     */
    @PostMapping
    public ResponseEntity<HealthStatusResponse> createHealthStatus(@Valid @RequestBody HealthStatusRequest request,
                                                                   @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(healthStatusService.createHealthStatus(user.getUserId(), request));
    }

    /*
     GET /users/me/health-status : 산모 건강 상태 조회
     */
    @GetMapping
    public ResponseEntity<HealthStatusResponse> getHealthStatus(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(healthStatusService.getHealthStatus(user.getUserId()));
    }

    /*
     PATCH /users/me/health-status : 산모 건강 상태 수정
     */
    @PatchMapping
    public ResponseEntity<HealthStatusResponse> updateHealthStatus(@Valid @RequestBody HealthStatusRequest request,
                                                                   @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(healthStatusService.updateHealthStatus(user.getUserId(), request));
    }
}
