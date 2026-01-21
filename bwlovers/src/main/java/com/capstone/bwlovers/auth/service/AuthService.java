package com.capstone.bwlovers.auth.service;

import com.capstone.bwlovers.auth.domain.OAuthProvider;
import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.auth.dto.request.UpdateNaverRequest;
import com.capstone.bwlovers.auth.dto.response.NaverUserInfoResponse;
import com.capstone.bwlovers.auth.dto.response.NaverTokenResponse;
import com.capstone.bwlovers.auth.dto.response.TokenResponse;
import com.capstone.bwlovers.auth.dto.response.UpdateNaverResponse;
import com.capstone.bwlovers.auth.repository.UserRepository;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.capstone.bwlovers.global.security.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

    public TokenResponse loginWithNaver(String code, String state) {

        System.out.println("====== 환경변수 체크 ======");
        System.out.println("clientId: " + clientId);
        System.out.println("clientSecret: " + clientSecret);
        System.out.println("redirectUri: " + redirectUri);
        System.out.println("==========================");

        if (code == null || code.isBlank()) throw new CustomException(ExceptionCode.ILLEGAL_ARGUMENT);

        // code -> naver access token
        NaverTokenResponse tokenRes = oAuthClient.requestToken(code, state, clientId, clientSecret, redirectUri);
        String naverAccessToken = tokenRes.getAccessToken();
        if (naverAccessToken == null || naverAccessToken.isBlank()) {
            throw new CustomException(ExceptionCode.AUTH_TOKEN_EMPTY);
        }

        // naver access token -> user info
        NaverUserInfoResponse userInfoRes = oAuthClient.requestUserInfo(naverAccessToken);
        if (userInfoRes == null || userInfoRes.getResponse() == null || userInfoRes.getResponse().getId() == null) {
            throw new CustomException(ExceptionCode.LOGIN_ERROR);
        }

        String providerId = userInfoRes.getResponse().getId();
        String email = userInfoRes.getResponse().getEmail();
        String name = userInfoRes.getResponse().getName();
        String mobile = userInfoRes.getResponse().getMobile();
        String profileImageUrl = userInfoRes.getResponse().getProfileImageUrl();

        // DB upsert
        User user = userRepository.findByProviderAndProviderId(OAuthProvider.NAVER, providerId)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .provider(OAuthProvider.NAVER)
                                .providerId(providerId)
                                .email(email == null ? "unknown@naver.com" : email) // email이 null일 수 있으면 방어
                                .username(name)
                                .phone(mobile)
                                .profileImageUrl(profileImageUrl != null ? profileImageUrl : "/images/default-profile.png")
                                .build()
                ));

        // JWT 발급 (subject는 유저 식별자로)
        String subject = user.getProvider().name() + ":" + user.getProviderId();

        return new TokenResponse(
                jwtProvider.createAccessToken(subject, DEFAULT_ROLES),
                jwtProvider.createRefreshToken(subject, DEFAULT_ROLES)
        );
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
    네이버 정보 수정
     */
    @Transactional
    public UpdateNaverResponse updateNaver(Long userId, UpdateNaverRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.update(request.getUsername(), user.getProfileImageUrl());
        }

        if (request.getProfileImageUrl() != null && !request.getProfileImageUrl().isBlank()) {
            user.update(user.getUsername(), request.getProfileImageUrl());
        }

        return new UpdateNaverResponse(user.getUsername(), user.getProfileImageUrl());
    }

}
