package com.capstone.bwlovers.health.repository;

import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.health.domain.HealthStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HealthStatusRepository extends JpaRepository<HealthStatus, Long> {
    Optional<HealthStatus> findByUser(User user);
}
