package com.capstone.bwlovers.simulation.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SimulationDetailListResponse {
    private Long simulationId;
    private String productName;
    private LocalDateTime createdAt;

    public static SimulationDetailListResponse of(Long simulationId, String productName, LocalDateTime createdAt) {
        return SimulationDetailListResponse.builder()
                .simulationId(simulationId)
                .productName(productName)
                .createdAt(createdAt)
                .build();
    }
}