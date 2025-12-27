package com.capstone.bwlovers.auth.dto.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NaverLoginRequest {
    private final String code;
    private final String state;
}