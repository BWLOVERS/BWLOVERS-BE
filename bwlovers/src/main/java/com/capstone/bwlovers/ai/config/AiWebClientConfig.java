package com.capstone.bwlovers.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AiWebClientConfig {

    @Value("${ai.server.url}")
    private String aiServerUrl;

    @Bean
    public WebClient aiWebClient() {
        return WebClient.builder()
                .baseUrl(aiServerUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}