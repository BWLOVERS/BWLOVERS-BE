package com.capstone.bwlovers.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MaternityProfileDto {

    private UserDto user;
    private PregnancyInfoDto pregnancyInfo;
    private HealthStatusDto healthStatus;

    @Getter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class UserDto {
        private Long user_id;
        private String name;
        private String email;
    }

    @Getter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class PregnancyInfoDto {
        private Integer age;
        private Integer height;
        private Integer weight_pre;
        private Integer weight_current;
        private Boolean is_firstbirth;
        private Integer gestational_week;
        private String expected_date;
        private Boolean is_multiple_pregnancy;
        private Integer miscarriage_history;
    }

    @Getter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class HealthStatusDto {
        private String past_history_json;
        private String medicine_json;
        private String current_condition;
        private String chronic_conditions_json;
        private String pregnancy_complications_json;
    }
}

