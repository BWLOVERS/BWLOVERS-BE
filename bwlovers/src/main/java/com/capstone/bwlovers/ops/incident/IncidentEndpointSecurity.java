package com.capstone.bwlovers.ops.incident;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Configuration(proxyBeanMethods = false)
public class IncidentEndpointSecurity {
    @Bean
    @Order(0)
    SecurityFilterChain incidentSecurity(HttpSecurity http, ObjectProvider<IncidentWebhookSettings> settings) throws Exception {
        // Always reserve these routes: when disabled, even a normal application JWT cannot use them.
        http.securityMatcher("/internal/ai-incidents", "/internal/ai-incidents/**", "/actuator/prometheus")
                .csrf(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(new TokenFilter(settings), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    static final class TokenFilter extends OncePerRequestFilter {
        private final ObjectProvider<IncidentWebhookSettings> settings;
        TokenFilter(ObjectProvider<IncidentWebhookSettings> settings) { this.settings = settings; }

        @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            IncidentWebhookSettings config = settings.getIfAvailable();
            if (config == null) { reject(response, 404, "INCIDENT_DISABLED"); return; }
            boolean metrics = request.getRequestURI().substring(request.getContextPath().length()).equals("/actuator/prometheus");
            String supplied = request.getHeader(metrics ? "Authorization" : "X-Alert-Token");
            if (!metrics && supplied == null) {
                String authorization = request.getHeader("Authorization");
                if (authorization != null && authorization.startsWith("Bearer ")) supplied = authorization.substring(7);
            }
            String expected = metrics ? "Bearer " + config.metricsToken() : config.token();
            if (supplied == null || supplied.length() > 300 || !MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
                reject(response, 401, "INVALID_ALERT_CREDENTIALS"); return;
            }
            chain.doFilter(request, response);
        }

        private static void reject(HttpServletResponse response, int status, String code) throws IOException {
            response.setStatus(status); response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"" + code + "\"}");
        }
    }
}
