package com.capstone.bwlovers.auth.service;

import com.capstone.bwlovers.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomOAuth2UserServiceTest {

    @Test
    void loadUser_convertsCustomExceptionToOAuth2AuthenticationException() {
        UserRepository userRepository = mock(UserRepository.class);
        OAuth2User providerUser = mock(OAuth2User.class);
        when(providerUser.getAttributes()).thenReturn(Map.of());

        OAuth2UserRequest userRequest = mock(OAuth2UserRequest.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        when(userRequest.getClientRegistration()).thenReturn(clientRegistration);
        when(clientRegistration.getRegistrationId()).thenReturn("naver");

        CustomOAuth2UserService service = new TestCustomOAuth2UserService(userRepository, providerUser);

        OAuth2AuthenticationException exception = assertThrows(
                OAuth2AuthenticationException.class,
                () -> service.loadUser(userRequest)
        );

        assertEquals("LOGIN_ERROR", exception.getError().getErrorCode());
        assertEquals("소셜 로그인 인증에 실패했습니다.", exception.getError().getDescription());
    }

    private static class TestCustomOAuth2UserService extends CustomOAuth2UserService {

        private final OAuth2User providerUser;

        private TestCustomOAuth2UserService(UserRepository userRepository, OAuth2User providerUser) {
            super(userRepository);
            this.providerUser = providerUser;
        }

        @Override
        protected OAuth2User loadProviderUser(OAuth2UserRequest userRequest) {
            return providerUser;
        }
    }
}
