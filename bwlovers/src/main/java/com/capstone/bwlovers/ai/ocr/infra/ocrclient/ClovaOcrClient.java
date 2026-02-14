package com.capstone.bwlovers.ai.ocr.infra.ocrclient;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Component
@RequiredArgsConstructor
public class ClovaOcrClient {

    private final WebClient aiWebClient;

    @Value("${analysis.ocr.clova.invoke-url}")
    private String invokeUrl;

    @Value("${analysis.ocr.clova.secret}")
    private String secret;

    public String extractTextByUrl(String imageUrl, String format) {

        Map<String, Object> body = Map.of(
                "images", List.of(Map.of(
                        "format", format,
                        "name", "page",
                        "url", imageUrl
                )),
                "lang", "ko",
                "requestId", UUID.randomUUID().toString(),
                "timestamp", System.currentTimeMillis(),
                "version", "V1"
        );

        Map resp = aiWebClient.post()
                .uri(invokeUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-OCR-SECRET", secret)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return parseInferText(resp);
    }

    @SuppressWarnings("unchecked")
    private String parseInferText(Map resp) {
        if (resp == null) return "";

        List<Map<String, Object>> images = (List<Map<String, Object>>) resp.get("images");
        if (images == null || images.isEmpty()) return "";

        List<Map<String, Object>> fields = (List<Map<String, Object>>) images.get(0).get("fields");
        if (fields == null) return "";

        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> f : fields) {
            Object t = f.get("inferText");
            if (t != null) sb.append(t).append(" ");
        }
        return sb.toString().trim();
    }
}