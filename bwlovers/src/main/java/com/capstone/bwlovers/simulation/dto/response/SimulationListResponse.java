package com.capstone.bwlovers.simulation.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SimulationListResponse {
    private Long simulationId;
    private LocalDateTime createdAt;

    public static SimulationListResponse of(Long simulationId, LocalDateTime createdAt) {
        return SimulationListResponse.builder()
                .simulationId(simulationId)
                .createdAt(createdAt)
                .build();
    }
}