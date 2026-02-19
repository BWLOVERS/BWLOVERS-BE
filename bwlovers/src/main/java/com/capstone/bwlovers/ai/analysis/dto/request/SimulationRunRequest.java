package com.capstone.bwlovers.ai.analysis.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SimulationRunRequest {

    @NotNull
    private Long insuranceId;

    @NotEmpty
    private List<Long> selectedContractIds;

    @NotBlank
    private String question;
}