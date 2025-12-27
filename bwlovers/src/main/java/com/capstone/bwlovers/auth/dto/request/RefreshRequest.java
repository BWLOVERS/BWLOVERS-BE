package com.capstone.bwlovers.auth.dto.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RefreshRequest {
    private final String refreshToken;
}
