package com.capstone.bwlovers.ops.incident;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "incident.webhook.enabled", havingValue = "true")
@EnableConfigurationProperties(IncidentWebhookSettings.class)
public class IncidentWebhookConfiguration {
    @Bean IncidentJobStore incidentJobStore(StringRedisTemplate redis) { return new RedisIncidentJobStore(redis); }

    @Bean(destroyMethod = "close")
    IncidentAnalysisWorker incidentWorker(IncidentJobStore store, IncidentWebhookSettings settings) throws IOException {
        IncidentHttp http = IncidentHttp.create();
        IncidentAnalysisWorker worker = new IncidentAnalysisWorker(store,
                new PrometheusIncidentCollector(PrometheusIncidentCollector.http(settings.prometheusUrl(), settings.prometheusToken())),
                new IncidentAnalyzer(http, settings.apiKey(), settings.model()), http, IncidentDiscord.webhook(settings.discordWebhookUrl()));
        if (settings.workerEnabled()) worker.start();
        return worker;
    }
}
