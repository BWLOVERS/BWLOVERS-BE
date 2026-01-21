package com.capstone.bwlovers.health.controller;

import com.capstone.bwlovers.health.dto.request.HealthStatusRequest;
import com.capstone.bwlovers.health.dto.response.HealthStatusResponse;
import com.capstone.bwlovers.health.service.HealthStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
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
                                                                   @AuthenticationPrincipal OAuth2User principal) {
        Long userId = principal.getAttribute("userId");
        return ResponseEntity.ok(healthStatusService.createHealthStatus(userId, request));
    }

    /*
     GET /users/me/health-status : 산모 건강 상태 조회
     */
    @GetMapping
    public ResponseEntity<HealthStatusResponse> getHealthStatus(@AuthenticationPrincipal OAuth2User principal) {
        Long userId = principal.getAttribute("userId");
        return ResponseEntity.ok(healthStatusService.getHealthStatus(userId));
    }

    /*
     PATCH /users/me/health-status : 산모 건강 상태 수정
     */
    @PatchMapping
    public ResponseEntity<HealthStatusResponse> updateHealthStatus(@Valid @RequestBody HealthStatusRequest request,
                                                                   @AuthenticationPrincipal OAuth2User principal) {
        Long userId = principal.getAttribute("userId");
        return ResponseEntity.ok(healthStatusService.updateHealthStatus(userId, request));
    }
}
