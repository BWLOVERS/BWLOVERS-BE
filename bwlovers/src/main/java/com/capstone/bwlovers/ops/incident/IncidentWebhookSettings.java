package com.capstone.bwlovers.ops.incident;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

import static com.capstone.bwlovers.ops.incident.IncidentJson.require;

@ConfigurationProperties("incident.webhook")
public record IncidentWebhookSettings(String token, String metricsToken, URI prometheusUrl, String prometheusToken,
                                      String job, String pool, String apiKey, String model, String discordWebhookUrl,
                                      Boolean workerEnabled) {
    public IncidentWebhookSettings {
        require(token != null && token.matches("[A-Za-z0-9_-]{32,256}"), "INCIDENT_ALERT_TOKEN은 32~256자의 영문·숫자·_·-여야 합니다.");
        require(metricsToken != null && metricsToken.matches("[A-Za-z0-9_-]{32,256}") && !metricsToken.equals(token),
                "INCIDENT_METRICS_TOKEN은 알림 토큰과 다른 32~256자의 영문·숫자·_·-여야 합니다.");
        require(prometheusUrl != null && ("http".equals(prometheusUrl.getScheme()) || "https".equals(prometheusUrl.getScheme()))
                && prometheusUrl.getHost() != null && prometheusUrl.getUserInfo() == null
                && prometheusUrl.getQuery() == null && prometheusUrl.getFragment() == null, "Prometheus URL 설정이 올바르지 않습니다.");
        require(apiKey != null && apiKey.matches("[\\x21-\\x7E]+"), "INCIDENT_OPENAI_API_KEY 설정이 필요합니다.");
        require(prometheusToken == null || prometheusToken.isEmpty() || prometheusToken.matches("[\\x21-\\x7E]+"), "Prometheus 토큰 형식이 올바르지 않습니다.");
        job = job == null ? "bwlovers" : job;
        pool = pool == null ? "HikariPool-1" : pool;
        model = model == null ? "gpt-4.1-mini" : model;
        workerEnabled = workerEnabled == null || workerEnabled;
        require(job.matches("[A-Za-z0-9._-]{1,100}") && pool.matches("[A-Za-z0-9._-]{1,100}"), "job/pool 설정이 올바르지 않습니다.");
        IncidentDiscord.webhook(discordWebhookUrl);
    }

    @Override public String toString() { return "IncidentWebhookSettings[credentials redacted]"; }
}
