package com.capstone.bwlovers.auth.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NaverLoginResponse { // 프론트엔드 응답 DTO
    private final boolean success;
    private final String accessToken;
    private final String refreshToken;
}
