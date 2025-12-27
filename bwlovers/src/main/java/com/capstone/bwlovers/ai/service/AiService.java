package com.capstone.bwlovers.ai.service;

import com.capstone.bwlovers.ai.dto.FastApiResponse;
import com.capstone.bwlovers.ai.dto.MaternityProfileDto;
import com.capstone.bwlovers.maternity.domain.HealthStatus;
import com.capstone.bwlovers.maternity.domain.PregnancyInfo;
import com.capstone.bwlovers.maternity.repository.HealthStatusRepository;
import com.capstone.bwlovers.maternity.repository.PregnancyInfoRepository;
import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


@Service
@RequiredArgsConstructor
public class AiService {

    private final UserRepository userRepository;
    private final PregnancyInfoRepository pregnancyInfoRepository;
    private final HealthStatusRepository healthStatusRepository;
    private final WebClient aiWebClient;

    public FastApiResponse requestAiRecommendation(Long userId) {

        // 유저, 임신, 건강 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        PregnancyInfo info = pregnancyInfoRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Pregnancy info not found"));

        HealthStatus health = healthStatusRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Health status not found"));


        // FastAPI로 보낼 DTO 변환
        MaternityProfileDto dto = MaternityProfileDto.builder()
                .user(MaternityProfileDto.UserDto.builder()
                        .user_id(user.getUserId())
                        .name(user.getUsername())
                        .email(user.getEmail())
                        .build()
                )
                .pregnancyInfo(MaternityProfileDto.PregnancyInfoDto.builder()
                        .age(info.getAge())
                        .height(info.getHeight())
                        .weight_pre(info.getWeightPre())
                        .weight_current(info.getWeightCurrent())
                        .is_firstbirth(info.getIsFirstbirth())
                        .gestational_week(info.getGestationalWeek())
                        .expected_date(info.getExpectedDate().toString())
                        .is_multiple_pregnancy(info.getIsMultiplePregnancy())
                        .miscarriage_history(info.getMiscarriageHistory())
                        .build()
                )
                .healthStatus(MaternityProfileDto.HealthStatusDto.builder()
                        .past_history_json(health.getPastHistory())
                        .medicine_json(health.getMedicine())
                        .current_condition(health.getCurrentCondition())
                        .chronic_conditions_json(health.getChronicConditions())
                        .pregnancy_complications_json(health.getPregnancyComplications())
                        .build()
                )
                .build();


        // FastAPI로 POST 요청
        return aiWebClient.post()
                .uri("/ai/recommend")
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(FastApiResponse.class)
                .block();
    }
}

