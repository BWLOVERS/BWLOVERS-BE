package com.capstone.bwlovers.global.security.oauth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OAuth2FailureHandlerTest {

    @Test
    void onAuthenticationFailure_redirectsWithOAuth2ErrorDetails() throws Exception {
        OAuth2FailureHandler handler = new OAuth2FailureHandler();
        ReflectionTestUtils.setField(handler, "redirectFailureUrl", "http://localhost:3000/auth/callback");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/redirect/naver");
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
                new OAuth2Error("CLUB_MEMBER_LOGIN_FORBIDDEN", "동아리 부원만 로그인할 수 있습니다.", null),
                "동아리 부원만 로그인할 수 있습니다."
        );

        handler.onAuthenticationFailure(request, response, exception);

        assertEquals(
                "http://localhost:3000/auth/callback?success=false&error=CLUB_MEMBER_LOGIN_FORBIDDEN&message=%EB%8F%99%EC%95%84%EB%A6%AC+%EB%B6%80%EC%9B%90%EB%A7%8C+%EB%A1%9C%EA%B7%B8%EC%9D%B8%ED%95%A0+%EC%88%98+%EC%9E%88%EC%8A%B5%EB%8B%88%EB%8B%A4.",
                response.getRedirectedUrl()
        );
    }

    @Test
    void onAuthenticationFailure_redirectsWithFallbackErrorCode() throws Exception {
        OAuth2FailureHandler handler = new OAuth2FailureHandler();
        ReflectionTestUtils.setField(handler, "redirectFailureUrl", "http://localhost:3000/auth/callback");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/redirect/naver");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException exception = new AuthenticationException("generic failure") {
        };

        handler.onAuthenticationFailure(request, response, exception);

        assertEquals(
                "http://localhost:3000/auth/callback?success=false&error=OAUTH_LOGIN_FAILED&message=generic+failure",
                response.getRedirectedUrl()
        );
    }
}
