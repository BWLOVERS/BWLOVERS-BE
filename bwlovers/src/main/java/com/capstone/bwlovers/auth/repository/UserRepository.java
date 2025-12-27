package com.capstone.bwlovers.auth.repository;

import com.capstone.bwlovers.auth.domain.OAuthProvider;
import com.capstone.bwlovers.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderId(OAuthProvider provider, String providerId);
}
