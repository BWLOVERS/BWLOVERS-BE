package com.capstone.bwlovers.pregnancy.dto.response;

import com.capstone.bwlovers.pregnancy.domain.Job;
import com.capstone.bwlovers.pregnancy.domain.PregnancyInfo;
import lombok.*;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PregnancyInfoResponse {

    private Long infoId;
    private Long userId;

    private LocalDate birthDate;
    private Integer height;
    private Integer weightPre;
    private Integer weightCurrent;
    private Boolean isFirstbirth;
    private Integer gestationalWeek;
    private LocalDate expectedDate;
    private Boolean isMultiplePregnancy;
    private Integer miscarriageHistory;
    private JobDto job;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JobDto {
        private Long jobId;
        private String jobName;
        private Integer riskLevel;

        public static JobDto from(Job job) {
            if (job == null) return null;
            return JobDto.builder()
                    .jobId(job.getJobId())
                    .jobName(job.getJobName())
                    .riskLevel(job.getRiskLevel())
                    .build();
        }
    }

    public static PregnancyInfoResponse from(PregnancyInfo info) {
        return PregnancyInfoResponse.builder()
                .infoId(info.getInfoId())
                .userId(info.getUser().getUserId())
                .birthDate(info.getBirthDate())
                .height(info.getHeight())
                .weightPre(info.getWeightPre())
                .weightCurrent(info.getWeightCurrent())
                .isFirstbirth(info.getIsFirstbirth())
                .gestationalWeek(info.getGestationalWeek())
                .expectedDate(info.getExpectedDate())
                .isMultiplePregnancy(info.getIsMultiplePregnancy())
                .miscarriageHistory(info.getMiscarriageHistory())
                .job(JobDto.from(info.getJob()))
                .build();
    }
}
