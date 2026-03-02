package com.capstone.bwlovers.auth.controller;

import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.auth.dto.request.NaverLoginRequest;
import com.capstone.bwlovers.auth.dto.request.RefreshRequest;
import com.capstone.bwlovers.auth.dto.request.UpdateUsernameRequest;
import com.capstone.bwlovers.auth.dto.response.TokenResponse;
import com.capstone.bwlovers.auth.dto.response.UpdateUsernameResponse;
import com.capstone.bwlovers.auth.dto.response.UserInfoResponse;
import com.capstone.bwlovers.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    네이버 로그인 정보 조회 (프로필 사진, 닉네임, 이메일, 전화번호)
     */
    @GetMapping("/users/me")
    public ResponseEntity<UserInfoResponse> getNaver(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authService.getNaver(user.getUserId()));
    }

    /*
    회원 프로필 이미지 수정
     */
    @PatchMapping(value = "/users/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateProfileImage(@AuthenticationPrincipal User user,
                                                   @RequestPart("image") MultipartFile image) {
        authService.updateProfileImage(user.getUserId(), image);
        return ResponseEntity.ok().build();
    }

    /*
    회원 닉네임 수정
     */
    @PatchMapping("/users/me/username")
    public ResponseEntity<UpdateUsernameResponse> updateNickname(@AuthenticationPrincipal User user,
                                                                 @RequestBody @Valid UpdateUsernameRequest request) {
        return ResponseEntity.ok(authService.updateUsername(user.getUserId(), request));
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
