package com.capstone.bwlovers.health.service;

import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.auth.repository.UserRepository;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.capstone.bwlovers.health.domain.*;
import com.capstone.bwlovers.health.dto.response.HealthStatusResponse;
import com.capstone.bwlovers.health.dto.request.HealthStatusRequest;
import com.capstone.bwlovers.health.repository.HealthStatusRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Transactional
public class HealthStatusService {

    private final HealthStatusRepository healthStatusRepository;
    private final UserRepository userRepository;

    /*
    산모 건강 상태 등록
     */
    @Transactional
    public HealthStatusResponse createHealthStatus(Long userId, HealthStatusRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        HealthStatus status = healthStatusRepository.findByUser(user)
                .orElseGet(() -> HealthStatus.builder()
                        .user(user)
                        .build());

        status.clearChildren();

        // 과거 병력
        if (request.getPastDiseases() != null) {
            for (HealthStatusRequest.PastDiseaseItem item : request.getPastDiseases()) {
                LocalDate treatedAt = parseYearMonthToFirstDay(item.getPastLastTreatedAt());

                PastDisease pd = PastDisease.builder()
                        .pastDiseaseType(item.getPastDiseaseType())
                        .pastCured(Boolean.TRUE.equals(item.getPastCured()))
                        .pastLastTreatedAt(treatedAt)
                        .build();

                status.addPastDisease(pd);
            }
        }

        // 만성 질환
        if (request.getChronicDiseases() != null) {
            for (HealthStatusRequest.ChronicDiseaseItem item : request.getChronicDiseases()) {
                ChronicDisease cd = ChronicDisease.builder()
                        .chronicDiseaseType(item.getChronicDiseaseType())
                        .chronicOnMedication(Boolean.TRUE.equals(item.getChronicOnMedication()))
                        .build();

                status.addChronicDisease(cd);
            }
        }

        // 이번 임신 확정 진단
        if (request.getPregnancyComplications() != null) {
            for (var item : request.getPregnancyComplications()) {
                PregnancyComplication pc = PregnancyComplication.builder()
                        .pregnancyComplicationType(item.getPregnancyComplicationType())
                        .build();
                status.addPregnancyComplication(pc);
            }
        }

        HealthStatus saved = healthStatusRepository.save(status);
        return HealthStatusResponse.from(saved);
    }

    /*
    산모 건강 정보 조회
     */
    @Transactional(readOnly = true)
    public HealthStatusResponse getHealthStatus(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
        HealthStatus status = healthStatusRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ExceptionCode.HEALTH_STATUS_NOT_FOUND));

        return HealthStatusResponse.from(status);
    }

    private LocalDate parseYearMonthToFirstDay(String ym) {
        YearMonth yearMonth = YearMonth.parse(ym);
        return yearMonth.atDay(1);
    }

    /*
    산모 건강 정보 수정
     */
    @Transactional
    public HealthStatusResponse updateHealthStatus(Long userId, HealthStatusRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        HealthStatus status = healthStatusRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ExceptionCode.HEALTH_STATUS_NOT_FOUND));

        // 기존 자식 엔티티 전체 삭제 후, 요청 값으로 재구성
        status.clearChildren();

        // 과거 병력
        if (request.getPastDiseases() != null) {
            for (HealthStatusRequest.PastDiseaseItem item : request.getPastDiseases()) {

                LocalDate treatedAt = parseYearMonthToFirstDay(item.getPastLastTreatedAt());

                PastDisease pd = PastDisease.builder()
                        .pastDiseaseType(item.getPastDiseaseType())
                        .pastCured(Boolean.TRUE.equals(item.getPastCured()))
                        .pastLastTreatedAt(treatedAt)
                        .build();

                status.addPastDisease(pd);
            }
        }

        // 만성 질환
        if (request.getChronicDiseases() != null) {
            for (HealthStatusRequest.ChronicDiseaseItem item : request.getChronicDiseases()) {

                ChronicDisease cd = ChronicDisease.builder()
                        .chronicDiseaseType(item.getChronicDiseaseType())
                        .chronicOnMedication(Boolean.TRUE.equals(item.getChronicOnMedication()))
                        .build();

                status.addChronicDisease(cd);
            }
        }

        // 이번 임신 확정 진단
        if (request.getPregnancyComplications() != null) {
            for (var item : request.getPregnancyComplications()) {
                if (item == null || item.getPregnancyComplicationType() == null) {
                    continue;
                }
                PregnancyComplication pc = PregnancyComplication.builder()
                        .pregnancyComplicationType(item.getPregnancyComplicationType())
                        .build();
                status.addPregnancyComplication(pc);
            }
        }

        HealthStatus saved = healthStatusRepository.save(status);
        return HealthStatusResponse.from(saved);
    }

}
