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

    private Long jobId;
    private String jobName;
    private Integer riskLevel;

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
                .jobId(info.getJob().getJobId())
                .jobName(info.getJob().getJobName())
                .riskLevel(info.getJob().getRiskLevel())
                .build();
    }
}
