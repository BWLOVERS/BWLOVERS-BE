package com.capstone.bwlovers.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NaverUserInfoResponse {
    private String resultcode;
    private String message;
    private Response response;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String email;
        private String name;

        @JsonProperty("mobile")
        private String mobile;

        private String profileImageUrl;
    }
}
