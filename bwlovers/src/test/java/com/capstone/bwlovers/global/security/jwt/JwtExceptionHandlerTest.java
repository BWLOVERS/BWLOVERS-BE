package com.capstone.bwlovers.global.security.jwt;

import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class JwtExceptionHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void commence_returnsExpiredCodeWhenJwtExceptionExists() throws Exception {
        JwtExceptionHandler handler = new JwtExceptionHandler(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setAttribute(JwtFilter.JWT_EXCEPTION_CODE_ATTR, ExceptionCode.AUTH_TOKEN_EXPIRED);

        handler.commence(request, response, mock(AuthenticationException.class));

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals(401, response.getStatus());
        assertEquals("AUTH_TOKEN_EXPIRED", body.get("code").asText());
        assertEquals("만료된 토큰입니다.", body.get("message").asText());
        assertEquals("/users/me", body.get("path").asText());
    }

    @Test
    void commence_returnsTokenEmptyCodeWhenJwtExceptionDoesNotExist() throws Exception {
        JwtExceptionHandler handler = new JwtExceptionHandler(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(request, response, mock(AuthenticationException.class));

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals(401, response.getStatus());
        assertEquals("AUTH_TOKEN_EMPTY", body.get("code").asText());
        assertEquals("인증 토큰이 존재하지 않습니다. 다시 로그인해주세요.", body.get("message").asText());
        assertEquals("/users/me", body.get("path").asText());
    }
}
