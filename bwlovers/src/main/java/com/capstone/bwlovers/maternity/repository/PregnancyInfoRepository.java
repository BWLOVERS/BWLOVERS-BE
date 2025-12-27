package com.capstone.bwlovers.maternity.repository;

import com.capstone.bwlovers.maternity.domain.PregnancyInfo;
import com.capstone.bwlovers.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PregnancyInfoRepository extends JpaRepository<PregnancyInfo, Long> {
    Optional<PregnancyInfo> findByUser(User user);
}
