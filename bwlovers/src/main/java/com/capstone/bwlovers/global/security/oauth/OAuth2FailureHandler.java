package com.capstone.bwlovers.global.security.oauth;

import com.capstone.bwlovers.global.exception.ExceptionCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.util.StringUtils;

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
        String message = exception.getMessage();

        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            if (StringUtils.hasText(oauth2Exception.getError().getErrorCode())) {
                error = URLEncoder.encode(oauth2Exception.getError().getErrorCode(), StandardCharsets.UTF_8);
            }
            if (StringUtils.hasText(oauth2Exception.getError().getDescription())) {
                message = oauth2Exception.getError().getDescription();
            }
        }

        if (!StringUtils.hasText(message)) {
            message = ExceptionCode.LOGIN_ERROR.getMessage();
        }

        String msg = URLEncoder.encode(message, StandardCharsets.UTF_8);

        String url = redirectFailureUrl
                + "?success=false"
                + "&error=" + error
                + "&message=" + msg;

        response.sendRedirect(url);
    }
}
