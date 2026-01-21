package com.capstone.bwlovers.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class UpdateNaverResponse {
    private String username;
    private String profileImageUrl;
}
