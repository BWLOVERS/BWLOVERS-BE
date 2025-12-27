package com.capstone.bwlovers.global.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    @Value("${oauth2.redirect.failure-url}")
    private String redirectFailureUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        String error = URLEncoder.encode("OAUTH_LOGIN_FAILED", StandardCharsets.UTF_8);
        String msg = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);

        String url = redirectFailureUrl
                + "?success=false"
                + "&error=" + error
                + "&message=" + msg;

        response.sendRedirect(url);
    }
}
