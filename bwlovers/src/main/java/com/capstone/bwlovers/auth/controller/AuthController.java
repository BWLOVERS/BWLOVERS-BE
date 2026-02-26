package com.capstone.bwlovers.auth.controller;

import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.auth.dto.request.NaverLoginRequest;
import com.capstone.bwlovers.auth.dto.request.RefreshRequest;
import com.capstone.bwlovers.auth.dto.request.UpdateNaverRequest;
import com.capstone.bwlovers.auth.dto.response.TokenResponse;
import com.capstone.bwlovers.auth.dto.response.UpdateNaverResponse;
import com.capstone.bwlovers.auth.service.AuthService;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/auth/redirect/naver")
    public ResponseEntity<Map<String, String>> redirectToNaver() {
        String uri = authService.getNaverRedirectUri();
        return ResponseEntity.ok(Map.of("redirectUri", uri));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid NaverLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithNaver(request.getCode(), request.getState()));
    }

    @PostMapping("/auth/refresh")
    public TokenResponse refresh(@RequestBody @Valid RefreshRequest request) {
        return authService.refreshTokens(request.getRefreshToken());
    }

    /*
    네이버 로그인 정보 수정 (닉네임, 프로필 사진만)
     */
    @PatchMapping("/users/naver")
    public ResponseEntity<UpdateNaverResponse> updateNaver(@AuthenticationPrincipal OAuth2User principal,
                                                           @RequestBody UpdateNaverRequest request) {
        Long userId = principal.getAttribute("userId");
        UpdateNaverResponse response = authService.updateNaver(userId, request);
        return ResponseEntity.ok(response);
    }

    /*
    회원 탈퇴
     */
    @DeleteMapping("/users/withdraw")
    public ResponseEntity<Void> withdraw(Authentication authentication) {
        authService.withdraw(authentication);
        return ResponseEntity.noContent().build();
    }

}
