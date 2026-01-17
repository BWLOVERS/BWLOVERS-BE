package com.capstone.bwlovers.pregnancy.dto.response;

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

    private java.util.List<JobDto> jobs;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JobDto {
        private Long jobId;
        private String jobName;
        private Integer riskLevel;
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
                .jobs(info.getJobs().stream()
                        .map(j -> JobDto.builder()
                                .jobId(j.getJobId())
                                .jobName(j.getJobName())
                                .riskLevel(j.getRiskLevel())
                                .build())
                        .toList())
                .build();
    }
}
