package com.capstone.bwlovers.ai.dto.request;

import com.capstone.bwlovers.health.dto.request.HealthStatusRequest;
import com.capstone.bwlovers.pregnancy.dto.request.PregnancyInfoRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FastApiRequest {
    private PregnancyInfoRequest pregnancyInfoRequest;
    private HealthStatusRequest healthStatusRequest;
}
