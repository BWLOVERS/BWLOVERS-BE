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
@RequestMapping("/users")
public class HealthStatusController {

    private final HealthStatusService healthStatusService;

    /*
    POST /me/health-status : 산모 건강 상태 등록
     */
    @PostMapping("/me/health-status")
    public ResponseEntity<HealthStatusResponse> createHealthStatue(@Valid @RequestBody HealthStatusRequest request,
                                                                   @AuthenticationPrincipal User user) {
        HealthStatusResponse response = healthStatusService.createHealthStatus(user.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    /*
    GET /me/health-status : 산모 건강 상태 조회
     */
    @GetMapping("/me/health-status")
    public ResponseEntity<HealthStatusResponse> getHealthStatus(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(healthStatusService.getHealthStatus(user.getUserId()));
    }

}


