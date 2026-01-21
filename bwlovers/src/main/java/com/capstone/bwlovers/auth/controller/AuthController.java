package com.capstone.bwlovers.auth.controller;

import com.capstone.bwlovers.auth.dto.request.NaverLoginRequest;
import com.capstone.bwlovers.auth.dto.request.RefreshRequest;
import com.capstone.bwlovers.auth.dto.request.UpdateNaverRequest;
import com.capstone.bwlovers.auth.dto.response.TokenResponse;
import com.capstone.bwlovers.auth.dto.response.UpdateNaverResponse;
import com.capstone.bwlovers.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
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

    /*
    네이버 로그인 정보 수정 (닉네임, 프로필 사진만)
     */
    @PatchMapping("/naver")
    public ResponseEntity<UpdateNaverResponse> updateNaver(@AuthenticationPrincipal OAuth2User principal,
                                                           @RequestBody UpdateNaverRequest request) {
        Long userId = principal.getAttribute("userId");
        UpdateNaverResponse response = authService.updateNaver(userId, request);
        return ResponseEntity.ok(response);
    }
}
