package com.capstone.bwlovers.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FastApiTicketResponse {
    @JsonProperty("result_id")
    private String resultId;

    @JsonProperty("expires_in_sec")
    private Integer expiresInSec;
}
