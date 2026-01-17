package com.capstone.bwlovers.pregnancy.service;

import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.auth.repository.UserRepository;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.capstone.bwlovers.pregnancy.domain.PregnancyInfo;
import com.capstone.bwlovers.pregnancy.dto.request.PregnancyInfoRequest;
import com.capstone.bwlovers.pregnancy.dto.response.PregnancyInfoResponse;
import com.capstone.bwlovers.pregnancy.repository.JobRepository;
import com.capstone.bwlovers.pregnancy.repository.PregnancyInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PregnancyInfoService {

    private final PregnancyInfoRepository pregnancyInfoRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    /*
    산모 기본 정보 생성
     */
    public PregnancyInfoResponse createPregnancyInfo(Long userId, PregnancyInfoRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        PregnancyInfo info = pregnancyInfoRepository.findByUser(user)
                .orElseGet(() -> PregnancyInfo.builder().user(user).build());

        info.update(
                request.getBirthDate(),
                request.getHeight(),
                request.getWeightPre(),
                request.getWeightCurrent(),
                request.getIsFirstbirth(),
                request.getGestationalWeek(),
                request.getExpectedDate(),
                request.getIsMultiplePregnancy(),
                request.getMiscarriageHistory()
        );

        info.clearJobs();
        var jobIds = request.getJobIds() == null ? java.util.List.<Long>of() : request.getJobIds();
        var jobs = jobRepository.findAllById(jobIds);

        if (jobs.size() != jobIds.size()) {
            throw new CustomException(ExceptionCode.JOB_NOT_FOUND);
        }
        jobs.forEach(info::addJob);

        return PregnancyInfoResponse.from(pregnancyInfoRepository.save(info));
    }

    /*
    산모 기본 정보 조회
     */
    @Transactional(readOnly = true)
    public PregnancyInfoResponse getPregnancyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        PregnancyInfo info = pregnancyInfoRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ExceptionCode.PREGNANCY_INFO_NOT_FOUND));

        return PregnancyInfoResponse.from(info);
    }

    /*
    산모 기본 정보 수정
     */
    public PregnancyInfoResponse updatePregnancyInfo(Long userId, PregnancyInfoRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
        PregnancyInfo info = pregnancyInfoRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ExceptionCode.PREGNANCY_INFO_NOT_FOUND));

        info.update(
                request.getBirthDate() != null ? request.getBirthDate() : info.getBirthDate(),
                request.getHeight() != null ? request.getHeight() : info.getHeight(),
                request.getWeightPre() != null ? request.getWeightPre() : info.getWeightPre(),
                request.getWeightCurrent() != null ? request.getWeightCurrent() : info.getWeightCurrent(),
                request.getIsFirstbirth() != null ? request.getIsFirstbirth() : info.getIsFirstbirth(),
                request.getGestationalWeek() != null ? request.getGestationalWeek() : info.getGestationalWeek(),
                request.getExpectedDate() != null ? request.getExpectedDate() : info.getExpectedDate(),
                request.getIsMultiplePregnancy() != null ? request.getIsMultiplePregnancy() : info.getIsMultiplePregnancy(),
                request.getMiscarriageHistory() != null ? request.getMiscarriageHistory() : info.getMiscarriageHistory()
        );

        if (request.getJobIds() != null) {
            info.clearJobs();

            var jobIds = request.getJobIds();
            var jobs = jobRepository.findAllById(jobIds);

            if (jobs.size() != jobIds.size()) {
                throw new CustomException(ExceptionCode.JOB_NOT_FOUND);
            }
            jobs.forEach(info::addJob);
        }

        pregnancyInfoRepository.save(info);
        return PregnancyInfoResponse.from(info);
    }
}
