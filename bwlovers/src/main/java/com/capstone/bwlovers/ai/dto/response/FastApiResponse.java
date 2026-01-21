package com.capstone.bwlovers.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class FastApiResponse {
    private boolean success;

    private Map<String, Object> profile;

    @JsonProperty("rag_result")
    private Map<String, Object> ragResult;

}
