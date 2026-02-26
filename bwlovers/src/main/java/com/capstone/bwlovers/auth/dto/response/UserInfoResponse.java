package com.capstone.bwlovers.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserInfoResponse {
    private String username;
    private String profileImageUrl;
    private String email;
    private String phone;
}