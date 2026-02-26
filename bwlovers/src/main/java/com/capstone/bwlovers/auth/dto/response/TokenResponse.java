package com.capstone.bwlovers.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public class TokenResponse {
    private final String accessToken;
    private final String refreshToken;

    @Setter
    @JsonProperty("isNew")
    private boolean isNew;
}