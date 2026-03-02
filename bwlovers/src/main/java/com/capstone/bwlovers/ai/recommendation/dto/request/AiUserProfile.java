package com.capstone.bwlovers.ai.recommendation.dto.request;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiUserProfile {
    private String birthDate;
    private Integer height;
    private Integer weightPre;
    private Integer weightCurrent;
    private Boolean isFirstbirth;
    private Integer gestationalWeek;
    private String expectedDate;
    private Boolean isMultiplePregnancy;
    private Integer miscarriageHistory;
    private String jobName;
    private Integer riskLevel;
}