package com.capstone.bwlovers.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateNaverRequest {
    private String username;
    private String profileImageUrl;
}
