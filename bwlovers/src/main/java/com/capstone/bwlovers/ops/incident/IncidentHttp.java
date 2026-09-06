package com.capstone.bwlovers.ops.incident;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static com.capstone.bwlovers.ops.incident.IncidentJson.*;

@FunctionalInterface
interface IncidentHttp {
    JsonNode post(URI uri, Map<String, String> headers, JsonNode body) throws IOException, InterruptedException;

    static IncidentHttp create() {
        // Never follow redirects carrying API credentials; there is deliberately no automatic POST retry.
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        return (uri, headers, body) -> {
            HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "BWLOVERS-Incident-Diagnosis/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)));
            headers.forEach(request::header);
            // Errors never expose body, URL or credentials.
            HttpResponse<String> response;
            try {
                response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                throw new Failure("외부 API 연결 실패 또는 시간 초과. 전송 성공 여부를 확인한 뒤 다시 실행하세요.");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new Failure("외부 API HTTP " + response.statusCode() + ". 자동 재시도하지 않았습니다.");
            }
            require(response.body().length() <= MAX_FILE_BYTES, "외부 API 응답이 너무 큽니다.");
            return response.body().isBlank() ? object() : parse(response.body());
        };
    }

    final class Failure extends IOException {
        Failure(String message) { super(message); }
    }
}
