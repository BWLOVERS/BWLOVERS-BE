package com.capstone.bwlovers.ops.incident;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static com.capstone.bwlovers.ops.incident.IncidentJson.*;

final class IncidentDiscord {
    private IncidentDiscord() { }

    static URI webhook(String value) {
        require(value != null && !value.isBlank(), "DISCORD_INCIDENT_WEBHOOK_URL 환경 변수가 필요합니다.");
        URI uri;
        try { uri = URI.create(value); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Discord Webhook URL 형식이 올바르지 않습니다."); }
        require("https".equals(uri.getScheme()) && "discord.com".equals(uri.getHost())
                        && uri.getPort() == -1 && uri.getUserInfo() == null && uri.getRawQuery() == null && uri.getFragment() == null
                        && uri.getRawPath().matches("/api/(?:v[0-9]+/)?webhooks/[0-9]+/[A-Za-z0-9._-]+"),
                "discord.com의 HTTPS Webhook URL을 query 없이 설정하세요. 일반 텍스트 채널을 사용합니다.");
        return URI.create(value + "?wait=true");
    }

    static String send(IncidentHttp http, URI webhook, JsonNode report) throws IOException, InterruptedException {
        JsonNode response = http.post(webhook, Map.of(), IncidentReport.discordPayload(report));
        require(response.path("id").isTextual() && response.path("id").asText().matches("[0-9]+"),
                "Discord 메시지 확인 응답이 없습니다. 채널에서 성공 여부를 확인하세요.");
        return response.path("id").asText();
    }
}
