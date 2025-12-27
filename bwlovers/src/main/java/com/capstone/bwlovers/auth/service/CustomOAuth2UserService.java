package com.capstone.bwlovers.auth.service;

import com.capstone.bwlovers.auth.domain.OAuthProvider;
import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.auth.repository.UserRepository;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
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

        OAuth2User oAuth2User = super.loadUser(userRequest);

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

        String subject = String.valueOf(user.getUserId());

        return new DefaultOAuth2User(
                List.of(() -> "ROLE_USER"),
                Map.of("subject", subject),
                "subject"
        );
    }

    private String get(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? null : v.toString();
    }
}
