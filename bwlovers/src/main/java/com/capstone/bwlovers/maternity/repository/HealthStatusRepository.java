package com.capstone.bwlovers.maternity.repository;

import com.capstone.bwlovers.maternity.domain.HealthStatus;
import com.capstone.bwlovers.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HealthStatusRepository extends JpaRepository<HealthStatus, Long> {
    Optional<HealthStatus> findByUser(User user);
}
