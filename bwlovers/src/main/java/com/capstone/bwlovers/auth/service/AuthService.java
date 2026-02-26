package com.capstone.bwlovers.auth.service;

import com.capstone.bwlovers.auth.domain.OAuthProvider;
import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.auth.dto.request.UpdateNaverRequest;
import com.capstone.bwlovers.auth.dto.response.*;
import com.capstone.bwlovers.auth.repository.UserRepository;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.capstone.bwlovers.global.security.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final List<String> DEFAULT_ROLES = List.of("ROLE_USER");

    private final JwtProvider jwtProvider;
    private final OAuthClient oAuthClient;
    private final UserRepository userRepository;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.naver.redirect-uri}")
    private String redirectUri;

    @Transactional
    public TokenResponse loginWithNaver(String code, String state) {

        // 네이버 토큰 요청
        NaverTokenResponse tokenRes = oAuthClient.requestToken(code, state, clientId, clientSecret, redirectUri);
        String naverAccessToken = tokenRes.getAccessToken();

        // 네이버 유저정보 요청
        NaverUserInfoResponse userInfoRes = oAuthClient.requestUserInfo(naverAccessToken);
        String providerId = userInfoRes.getResponse().getId();

        User user = userRepository.findByProviderAndProviderId(OAuthProvider.NAVER, providerId)
                .orElse(null);

        boolean isNew;

        if (user == null) {
            isNew = true;
            user = userRepository.save(
                    User.builder()
                            .provider(OAuthProvider.NAVER)
                            .providerId(providerId)
                            .email(userInfoRes.getResponse().getEmail())
                            .username(userInfoRes.getResponse().getName())
                            .phone(userInfoRes.getResponse().getMobile())
                            .profileImageUrl(userInfoRes.getResponse().getProfileImageUrl())
                            .naverAccessToken(naverAccessToken)
                            .build()
            );
        } else {
            isNew = false;
            user.updateNaverToken(naverAccessToken);
        }

        String subject = user.getProvider().name() + ":" + user.getProviderId();
        TokenResponse response = createTokenResponse(subject);

        response.setNew(isNew);
        return response;
    }

    public TokenResponse refreshTokens(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException(ExceptionCode.REFRESH_TOKEN_EMPTY);
        }

        // 만료/위조면 parseClaims에서 CustomException 터짐(위 parseClaims 적용 시)
        Claims claims = jwtProvider.parseClaims(refreshToken);

        // refresh 토큰인지 확인(typ)
        String typ = claims.get("typ", String.class);
        if (!"refresh".equals(typ)) {
            throw new CustomException(ExceptionCode.AUTH_TOKEN_INVALID);
        }

        String subject = claims.getSubject();
        return createTokenResponse(subject);
    }

    private TokenResponse createTokenResponse(String subject) {
        return new TokenResponse(
                jwtProvider.createAccessToken(subject, DEFAULT_ROLES),
                jwtProvider.createRefreshToken(subject, DEFAULT_ROLES)
        );
    }

    public String getNaverRedirectUri() {
        // 보안을 위한 랜덤 state 생성 (현재는 정적으로 설정해둠)
        String state = UUID.randomUUID().toString().replace("-", "");

        // (선택사항) 나중에 검증하고 싶다면 이 state를 Redis나 세션에 잠시 저장해둡니다.
        // redisTemplate.opsForValue().set("STATE:" + state, "valid", Duration.ofMinutes(5));

        // 네이버 인가 코드 요청 주소 조립
        return "https://nid.naver.com/oauth2.0/authorize" +
                "?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&state=" + state;
    }

    /*
    네이버 정보 조회
     */
    @Transactional(readOnly = true)
    public UserInfoResponse getNaver(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ExceptionCode.AUTH_TOKEN_INVALID);
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User user)) {
            throw new CustomException(ExceptionCode.AUTH_TOKEN_INVALID);
        }

        return UserInfoResponse.builder()
                .username(user.getUsername())
                .profileImageUrl(user.getProfileImageUrl())
                .phone(user.getPhone())
                .email(user.getEmail())
                .build();
    }

    /*
    네이버 정보 수정
     */
    @Transactional
    public UpdateNaverResponse updateNaver(Authentication authentication,
                                           UpdateNaverRequest request) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ExceptionCode.AUTH_TOKEN_INVALID);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User user)) {
            throw new CustomException(ExceptionCode.AUTH_TOKEN_INVALID);
        }

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.update(request.getUsername(), user.getProfileImageUrl());
        }

        if (request.getProfileImageUrl() != null && !request.getProfileImageUrl().isBlank()) {
            user.update(user.getUsername(), request.getProfileImageUrl());
        }

        return new UpdateNaverResponse(
                user.getUsername(),
                user.getProfileImageUrl()
        );
    }

    /*
    회원 탈퇴
     */
    @Transactional
    public void withdrawBySubject(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new CustomException(ExceptionCode.AUTH_TOKEN_INVALID);
        }

        String[] parts = subject.split(":", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new CustomException(ExceptionCode.AUTH_TOKEN_INVALID);
        }

        OAuthProvider provider;
        try {
            provider = OAuthProvider.valueOf(parts[0]);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ExceptionCode.AUTH_TOKEN_INVALID);
        }

        String providerId = parts[1];

        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        // 네이버 연동 해제 (best-effort 권장)
        if (user.getNaverAccessToken() != null && !user.getNaverAccessToken().isBlank()) {
            try {
                oAuthClient.deleteNaverLink(user.getNaverAccessToken(), clientId, clientSecret);
            } catch (Exception e) {
                log.warn("[WITHDRAW] NAVER unlink failed. userId={}, providerId={}, reason={}",
                        user.getUserId(),
                        user.getProviderId(),
                        e.getMessage()
                );
            }
        }

        // 연관관계 끊기 (FK / orphan 제거 안정화)
        user.clearRelations();
        userRepository.delete(user);
    }

    @Transactional
    public void withdraw(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ExceptionCode.AUTH_TOKEN_INVALID);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User user)) {
            throw new CustomException(ExceptionCode.AUTH_TOKEN_INVALID);
        }

        String subject = user.getProvider().name() + ":" + user.getProviderId();
        withdrawBySubject(subject);
    }



}
