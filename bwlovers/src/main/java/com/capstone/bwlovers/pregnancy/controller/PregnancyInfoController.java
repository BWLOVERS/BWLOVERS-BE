package com.capstone.bwlovers.pregnancy.controller;

import com.capstone.bwlovers.pregnancy.dto.request.PregnancyInfoRequest;
import com.capstone.bwlovers.pregnancy.dto.response.PregnancyInfoResponse;
import com.capstone.bwlovers.pregnancy.service.PregnancyInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/me/pregnancy-info")
public class PregnancyInfoController {

    private final PregnancyInfoService pregnancyInfoService;

    /*
     POST /users/me/pregnancy-info : 산모 기본 정보 등록
     */
    @PostMapping
    public ResponseEntity<PregnancyInfoResponse> createPregnancyInfo(@RequestBody PregnancyInfoRequest request,
                                                                     @AuthenticationPrincipal OAuth2User principal) {
        Long userId = principal.getAttribute("userId");
        return ResponseEntity.ok(pregnancyInfoService.createPregnancyInfo(userId, request));
    }

    /*
     GET /users/me/pregnancy-info : 산모 기본 정보 조회
     */
    @GetMapping
    public ResponseEntity<PregnancyInfoResponse> getPregnancyInfo(@AuthenticationPrincipal OAuth2User principal) {
        Long userId = principal.getAttribute("userId");
        return ResponseEntity.ok(pregnancyInfoService.getPregnancyInfo(userId));
    }

    /*
     PATCH /users/me/pregnancy-info : 산모 기본 정보 수정
     */
    @PatchMapping
    public ResponseEntity<PregnancyInfoResponse> updatePregnancyInfo(@RequestBody PregnancyInfoRequest request,
                                                                     @AuthenticationPrincipal OAuth2User principal) {
        Long userId = principal.getAttribute("userId");
        return ResponseEntity.ok(pregnancyInfoService.updatePregnancyInfo(userId, request));
    }
}
