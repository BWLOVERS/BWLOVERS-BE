package com.capstone.bwlovers.pregnancy.repository;

import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.pregnancy.domain.PregnancyInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PregnancyInfoRepository extends JpaRepository<PregnancyInfo, Long> {
    Optional<PregnancyInfo> findByUser(User user);
}