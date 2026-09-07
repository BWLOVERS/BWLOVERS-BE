package com.capstone.bwlovers.auth.service;

import com.capstone.bwlovers.auth.domain.OAuthProvider;
import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.auth.repository.UserRepository;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        try {
            OAuth2User oAuth2User = loadProviderUser(userRequest);

            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            if (!"naver".equals(registrationId)) {
                throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
            }

            Map<String, Object> attributes = oAuth2User.getAttributes();
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            if (response == null) {
                throw new CustomException(ExceptionCode.LOGIN_ERROR);
            }

            String providerId = get(response, "id");
            String name = get(response, "name");
            String email = get(response, "email");
            String mobile = get(response, "mobile");

            if (providerId == null || providerId.isBlank()) {
                throw new CustomException(ExceptionCode.LOGIN_ERROR);
            }

            User user = userRepository
                    .findByProviderAndProviderId(OAuthProvider.NAVER, providerId)
                    .orElseGet(() -> userRepository.save(
                            User.builder()
                                    .provider(OAuthProvider.NAVER)
                                    .providerId(providerId)
                                    .username(name)
                                    .email(email)
                                    .phone(mobile)
                                    .build()
                    ));

            return new DefaultOAuth2User(
                    List.of(() -> "ROLE_USER"),
                    Map.of("userId", user.getUserId()),
                    "userId"
            );
        } catch (CustomException e) {
            throw toOAuth2AuthenticationException(e);
        }
    }

    protected OAuth2User loadProviderUser(OAuth2UserRequest userRequest) {
        return super.loadUser(userRequest);
    }

    private String get(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? null : v.toString();
    }

    private OAuth2AuthenticationException toOAuth2AuthenticationException(CustomException e) {
        OAuth2Error error = new OAuth2Error(
                e.getExceptionCodeName(),
                e.getMessage(),
                null
        );
        return new OAuth2AuthenticationException(error, e.getMessage());
    }
}
