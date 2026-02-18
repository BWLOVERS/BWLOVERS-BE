package com.capstone.bwlovers.simulation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimulationSaveRequest {

    @NotBlank
    private String resultId;
}