package com.capstone.bwlovers.simulation.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SimulationSaveResponse {

    private Long simulationId;
    private String resultId;

    public static SimulationSaveResponse of(Long simulationId, String resultId) {
        return SimulationSaveResponse.builder()
                .simulationId(simulationId)
                .resultId(resultId)
                .build();
    }
}