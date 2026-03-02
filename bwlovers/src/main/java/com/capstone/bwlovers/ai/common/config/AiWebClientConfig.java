package com.capstone.bwlovers.ai.common.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class AiWebClientConfig {

    @Value("${ai.server.url}")
    private String aiServerUrl;

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int RESPONSE_TIMEOUT_SEC = 60;
    private static final int READ_TIMEOUT_SEC = 60;
    private static final int WRITE_TIMEOUT_SEC = 60;

    @Bean
    public WebClient aiWebClient() {

        ConnectionProvider provider = ConnectionProvider.builder("ai-webclient")
                .maxConnections(200)
                .pendingAcquireTimeout(Duration.ofSeconds(10))
                .pendingAcquireMaxCount(500)
                .maxIdleTime(Duration.ofSeconds(30))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .responseTimeout(Duration.ofSeconds(RESPONSE_TIMEOUT_SEC))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT_SEC, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(WRITE_TIMEOUT_SEC, TimeUnit.SECONDS))
                );

        return WebClient.builder()
                .baseUrl(aiServerUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(req -> {
            System.out.println("[AI WebClient] --> " + req.method() + " " + req.url());
            return reactor.core.publisher.Mono.just(req);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(res -> {
            System.out.println("[AI WebClient] <-- status=" + res.statusCode());
            return reactor.core.publisher.Mono.just(res);
        });
    }
}