package com.capstone.bwlovers.global.security.oauth;

import com.capstone.bwlovers.global.security.jwt.JwtProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;

    @Value("${oauth2.redirect.success-url}")
    private String redirectSuccessUrl;

    public OAuth2SuccessHandler(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        String subject = authentication.getName();

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String accessToken = jwtProvider.createAccessToken(subject, roles);
        String refreshToken = jwtProvider.createRefreshToken(subject, roles);

        // 프론트로 전달(쿼리스트링)
        String url = redirectSuccessUrl
                + "?success=true"
                + "&accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                + "&refreshToken=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

        response.sendRedirect(url);
    }
}
