package com.capstone.bwlovers.ai.recommendation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class FastApiResponse {

    @JsonProperty("result_id")
    private String resultId;

    @JsonProperty("expires_in_sec")
    private Long expiresInSec;
}
