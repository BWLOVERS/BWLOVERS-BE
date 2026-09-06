package com.capstone.bwlovers.global.security.jwt;

import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_setsAuthenticationWhenAccessTokenIsValid() throws Exception {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        JwtFilter jwtFilter = new JwtFilter(jwtProvider);
        FilterChain filterChain = mock(FilterChain.class);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Authentication authentication =
                new UsernamePasswordAuthenticationToken("user", null);
        when(jwtProvider.getAuthentication("valid-access-token")).thenReturn(authentication);

        jwtFilter.doFilter(request, response, filterChain);

        verify(jwtProvider).validateAccessToken("valid-access-token");
        verify(jwtProvider).getAuthentication("valid-access-token");
        verify(filterChain).doFilter(request, response);
        assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());
        assertNull(request.getAttribute(JwtFilter.JWT_EXCEPTION_CODE_ATTR));
    }

    @Test
    void doFilter_storesExceptionCodeWhenAccessTokenIsExpired() throws Exception {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        JwtFilter jwtFilter = new JwtFilter(jwtProvider);
        FilterChain filterChain = mock(FilterChain.class);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer expired-access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doThrow(new CustomException(ExceptionCode.AUTH_TOKEN_EXPIRED))
                .when(jwtProvider)
                .validateAccessToken("expired-access-token");

        jwtFilter.doFilter(request, response, filterChain);

        verify(jwtProvider).validateAccessToken("expired-access-token");
        verify(jwtProvider, never()).getAuthentication("expired-access-token");
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(
                ExceptionCode.AUTH_TOKEN_EXPIRED,
                request.getAttribute(JwtFilter.JWT_EXCEPTION_CODE_ATTR)
        );
    }
}
