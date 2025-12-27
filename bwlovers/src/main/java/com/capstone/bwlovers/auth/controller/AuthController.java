package com.capstone.bwlovers.auth.controller;

import com.capstone.bwlovers.auth.dto.request.NaverLoginRequest;
import com.capstone.bwlovers.auth.dto.request.RefreshRequest;
import com.capstone.bwlovers.auth.dto.response.TokenResponse;
import com.capstone.bwlovers.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/redirect/naver")
    public ResponseEntity<Map<String, String>> redirectToNaver() {
        String uri = authService.getNaverRedirectUri();
        return ResponseEntity.ok(Map.of("redirectUri", uri));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid NaverLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithNaver(request.getCode(), request.getState()));
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@RequestBody @Valid RefreshRequest request) {
        return authService.refreshTokens(request.getRefreshToken());
    }

}
