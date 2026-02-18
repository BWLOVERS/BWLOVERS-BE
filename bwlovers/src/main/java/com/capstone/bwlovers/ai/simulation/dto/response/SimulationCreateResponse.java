package com.capstone.bwlovers.ai.simulation.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SimulationCreateResponse {

    private String simulationId;

    public static SimulationCreateResponse of(String simulationId) {
        return SimulationCreateResponse.builder()
                .simulationId(simulationId)
                .build();
    }
}