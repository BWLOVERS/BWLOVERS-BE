package com.capstone.bwlovers.ai.service;

import com.capstone.bwlovers.ai.dto.request.FastApiRequest;
import com.capstone.bwlovers.ai.dto.response.FastApiResponse;
import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.auth.repository.UserRepository;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.capstone.bwlovers.health.domain.HealthStatus;
import com.capstone.bwlovers.health.dto.request.HealthStatusRequest;
import com.capstone.bwlovers.health.repository.HealthStatusRepository;
import com.capstone.bwlovers.pregnancy.domain.PregnancyInfo;
import com.capstone.bwlovers.pregnancy.dto.request.PregnancyInfoRequest;
import com.capstone.bwlovers.pregnancy.repository.PregnancyInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AiService {

    private final UserRepository userRepository;
    private final PregnancyInfoRepository pregnancyInfoRepository;
    private final HealthStatusRepository healthStatusRepository;
    private final WebClient aiWebClient;

    public FastApiResponse requestAiRecommendation(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
        PregnancyInfo pregnancyInfo = pregnancyInfoRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ExceptionCode.PREGNANCY_INFO_NOT_FOUND));
        HealthStatus healthStatus = healthStatusRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ExceptionCode.HEALTH_STATUS_NOT_FOUND));

        FastApiRequest dto = toFastApiRequest(pregnancyInfo, healthStatus);

        return aiWebClient.post()
                .uri("/ai/recommend")
                .bodyValue(dto)
                .retrieve()

                // 400, 422 → 요청 자체가 잘못됨
                .onStatus(
                        status -> status.value() == 400 || status.value() == 422,
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new CustomException(ExceptionCode.AI_INVALID_REQUEST)))
                )

                // 409 → AI가 분석을 수행할 수 없음 (조건 부족 등)
                .onStatus(
                        status -> status.value() == 409,
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new CustomException(ExceptionCode.AI_PROCESSING_FAILED)))
                )

                // 500대 → AI 서버 내부 오류
                .onStatus(
                        status -> status.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new CustomException(ExceptionCode.AI_SERVER_5XX)))
                )

                .bodyToMono(FastApiResponse.class)
                .timeout(Duration.ofSeconds(25))
                .block();

    }

    private FastApiRequest toFastApiRequest(PregnancyInfo pregnancyInfo, HealthStatus healthStatus) {

        PregnancyInfoRequest pregnancyInfoRequest = PregnancyInfoRequest.from(pregnancyInfo);
        HealthStatusRequest healthStatusRequest = HealthStatusRequest.from(healthStatus);

        return new FastApiRequest(pregnancyInfoRequest, healthStatusRequest);
    }
}
