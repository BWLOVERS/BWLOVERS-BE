package com.capstone.bwlovers.user.controller;

import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.user.dto.request.HealthStatusRequest;
import com.capstone.bwlovers.user.dto.response.HealthStatusResponse;
import com.capstone.bwlovers.user.service.HealthStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class HealthStatusController {

    private final HealthStatusService healthStatusService;

    @PostMapping("/me/health-status")
    public HealthStatusResponse createHealthStatus(@RequestBody HealthStatusRequest request,
                                                   @AuthenticationPrincipal User user) {
        return healthStatusService.createHealthStatus(user.getUserId(), request);
    }

    @PatchMapping("/me/health-status")
    public HealthStatusResponse updateHealthStatus(@RequestBody HealthStatusRequest request,
                                                   @AuthenticationPrincipal User user) {
        return healthStatusService.updateHealthStatus(user.getUserId(), request);
    }

    @GetMapping("/me/health-status")
    public HealthStatusResponse getHealthStatus(@AuthenticationPrincipal User user) {
        return healthStatusService.getHealthStatus(user.getUserId());
    }
}


