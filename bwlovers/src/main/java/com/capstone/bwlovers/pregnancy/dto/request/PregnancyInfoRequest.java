package com.capstone.bwlovers.pregnancy.dto.request;

import com.capstone.bwlovers.pregnancy.domain.PregnancyInfo;
import lombok.*;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PregnancyInfoRequest {

    private LocalDate birthDate;
    private Integer height;
    private Integer weightPre;
    private Integer weightCurrent;
    private Boolean isFirstbirth;
    private Integer gestationalWeek;
    private LocalDate expectedDate;
    private Boolean isMultiplePregnancy;
    private Integer miscarriageHistory;
    private java.util.List<Long> jobIds;

    public static PregnancyInfoRequest from(PregnancyInfo pregnancyInfo) {
        return PregnancyInfoRequest.builder()
                .birthDate(pregnancyInfo.getBirthDate())
                .height(pregnancyInfo.getHeight())
                .weightPre(pregnancyInfo.getWeightPre())
                .weightCurrent(pregnancyInfo.getWeightCurrent())
                .isFirstbirth(pregnancyInfo.getIsFirstbirth())
                .gestationalWeek(pregnancyInfo.getGestationalWeek())
                .expectedDate(pregnancyInfo.getExpectedDate())
                .isMultiplePregnancy(pregnancyInfo.getIsMultiplePregnancy())
                .miscarriageHistory(pregnancyInfo.getMiscarriageHistory())
                .jobIds(
                        pregnancyInfo.getJobs().stream()
                                .map(job -> job.getJobId())
                                .toList()
                )
                .build();
    }
}
