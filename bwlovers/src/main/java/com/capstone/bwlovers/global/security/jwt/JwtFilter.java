package com.capstone.bwlovers.global.security.jwt;

import com.capstone.bwlovers.global.exception.CustomException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtFilter extends OncePerRequestFilter {

    public static final String JWT_EXCEPTION_CODE_ATTR = "jwtExceptionCode";

    private final JwtProvider jwtProvider;

    public JwtFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveBearerToken(request);

        if (StringUtils.hasText(token)) {
            try {
                jwtProvider.validateAccessToken(token);
                SecurityContextHolder.getContext().setAuthentication(jwtProvider.getAuthentication(token));
            } catch (CustomException e) {
                SecurityContextHolder.clearContext();
                request.setAttribute(JWT_EXCEPTION_CODE_ATTR, e.getExceptionCode());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header)) return null;

        if (header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
