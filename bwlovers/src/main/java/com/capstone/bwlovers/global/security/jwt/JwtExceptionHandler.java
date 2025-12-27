package com.capstone.bwlovers.global.security.jwt;

import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JwtExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 401: 인증 실패 (토큰 없음/만료 등)
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        writeJson(response, ExceptionCode.AUTH_TOKEN_EMPTY, request.getRequestURI());
    }

    // 403: 인가 실패 (권한 부족)
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        writeJson(response, ExceptionCode.FORBIDDEN, request.getRequestURI());
    }

    /**
     * 공통 에러 응답 작성 메서드
     */
    private void writeJson(HttpServletResponse response, ExceptionCode exceptionCode, String path) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();

        // 1. 공통 규격 데이터 구성
        body.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        body.put("status", exceptionCode.getHttpStatus().value());
        body.put("code", exceptionCode.getClientExceptionCode().name()); // Enum 이름 출력
        body.put("message", exceptionCode.getMessage());
        body.put("path", path);

        // 2. HTTP 응답 설정
        response.setStatus(exceptionCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 3. JSON 변환 및 전송
        objectMapper.writeValue(response.getWriter(), body);
    }
}