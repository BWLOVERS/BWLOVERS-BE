package com.capstone.bwlovers.user.repository;

import com.capstone.bwlovers.user.domain.HealthStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HealthStatusRepository extends JpaRepository<HealthStatus, Long> {
    Optional<HealthStatus> findByUser_UserId(Long userId);
    boolean existsByUser_UserId(Long userId);
}
