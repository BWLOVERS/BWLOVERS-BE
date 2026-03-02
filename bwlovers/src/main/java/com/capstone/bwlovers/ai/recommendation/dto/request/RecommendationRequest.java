package com.capstone.bwlovers.ai.recommendation.dto.request;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationRequest {
    private AiUserProfile pregnancyInfo;
    private AiHealthStatus healthStatus;
}