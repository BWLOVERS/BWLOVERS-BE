package com.capstone.bwlovers.user.service;

import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.auth.repository.UserRepository;
import com.capstone.bwlovers.user.domain.HealthStatus;
import com.capstone.bwlovers.user.dto.request.HealthStatusRequest;
import com.capstone.bwlovers.user.dto.response.HealthStatusResponse;
import com.capstone.bwlovers.user.repository.HealthStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HealthStatusService {

    private final HealthStatusRepository healthStatusRepository;
    private final UserRepository userRepository;

    @Transactional
    public HealthStatusResponse createHealthStatus(Long userId, HealthStatusRequest request) {

        // 1) 유저 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않음"));

        // 2) 이미 healthStatus 존재하면 생성 불가
        if (healthStatusRepository.existsByUser_UserId(userId)) {
            throw new IllegalStateException("이미 건강 상태 정보가 존재함");
        }

        // 3) HealthStatus 생성 + 값 세팅
        HealthStatus healthStatus = HealthStatus.builder().build();
        healthStatus.updateHealthStatus(
                request.getPastCured(),
                request.getPastLastTreatedAt(),
                request.getPastDiseases(),
                request.getChronicDiseases(),
                request.getPregnancyComplications()
        );

        // 4) 연관관계 세팅 (여기서 healthStatus.setUser(user)까지 같이 걸려야 함)
        user.setHealthStatus(healthStatus);

        // 5) 저장
        HealthStatus saved = healthStatusRepository.save(healthStatus);
        return HealthStatusResponse.from(saved);
    }

    @Transactional
    public HealthStatusResponse updateHealthStatus(Long userId, HealthStatusRequest request) {

        HealthStatus hs = healthStatusRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("건강 상태 정보가 없음 (먼저 생성 필요함)"));

        hs.updateHealthStatus(
                request.getPastCured(),
                request.getPastLastTreatedAt(),
                request.getPastDiseases(),
                request.getChronicDiseases(),
                request.getPregnancyComplications()
        );

        // dirty checking이면 save 생략 가능. 명시해도 OK
        HealthStatus saved = healthStatusRepository.save(hs);
        return HealthStatusResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public HealthStatusResponse getHealthStatus(Long userId)  {

        HealthStatus healthStatus = healthStatusRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("건강 상태 정보가 없음"));

        return HealthStatusResponse.from(healthStatus);
    }
}
