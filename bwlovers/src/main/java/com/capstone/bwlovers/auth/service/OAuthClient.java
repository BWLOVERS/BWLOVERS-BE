package com.capstone.bwlovers.auth.service;

import com.capstone.bwlovers.auth.dto.response.NaverTokenResponse;
import com.capstone.bwlovers.auth.dto.response.NaverUserInfoResponse;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthClient {

    private final RestClient restClient;

    /**
     * code -> 네이버 토큰 응답
     */
    public NaverTokenResponse requestToken(String code, String state, String clientId, String clientSecret, String redirectUri) {
        if (code == null || code.isBlank()) {
            throw new CustomException(ExceptionCode.ILLEGAL_ARGUMENT);
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);
        body.add("state", state);

        try {
            NaverTokenResponse token = restClient.post()
                    .uri("https://nid.naver.com/oauth2.0/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(NaverTokenResponse.class);
            log.info("[NAVER RAW RESPONSE] {}", token);

            if (token == null || token.getAccessToken() == null || token.getAccessToken().isBlank()) {
                throw new CustomException(ExceptionCode.AUTH_TOKEN_EMPTY);
            }

            return token;

        } catch (RestClientResponseException e) {
            log.warn("[NAVER TOKEN FAIL] status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ExceptionCode.AUTH_SERVER_ERROR);
        } catch (Exception e) {
            log.error("[NAVER TOKEN ERROR]", e);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 네이버 access_token -> 네이버 유저정보 응답
     */
    public NaverUserInfoResponse requestUserInfo(String naverAccessToken) {
        if (naverAccessToken == null || naverAccessToken.isBlank()) {
            throw new CustomException(ExceptionCode.AUTH_TOKEN_EMPTY);
        }

        try {
            NaverUserInfoResponse userInfo = restClient.get()
                    .uri("https://openapi.naver.com/v1/nid/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + naverAccessToken)
                    .retrieve()
                    .body(NaverUserInfoResponse.class);

            if (userInfo == null || userInfo.getResponse() == null || userInfo.getResponse().getId() == null) {
                throw new CustomException(ExceptionCode.LOGIN_ERROR);
            }

            return userInfo;

        } catch (RestClientResponseException e) {
            log.warn("[NAVER USERINFO FAIL] status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            // 네이버 서버가 401/403/500 등을 주는 케이스
            throw new CustomException(ExceptionCode.AUTH_SERVER_ERROR);
        } catch (Exception e) {
            log.error("[NAVER USERINFO ERROR]", e);
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }
    }
}
