package com.capstone.bwlovers.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AiRecommendTicketResponse {
    private String resultId;
    private int expiresInSec;
}
