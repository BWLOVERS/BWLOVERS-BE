package com.capstone.bwlovers.ai.recommendation.dto.request;

import com.capstone.bwlovers.health.dto.request.HealthStatusRequest;
import com.capstone.bwlovers.pregnancy.dto.request.PregnancyInfoRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecommendationRequest {
    private PregnancyInfoRequest user_profile;
    private HealthStatusRequest health_status;
}
