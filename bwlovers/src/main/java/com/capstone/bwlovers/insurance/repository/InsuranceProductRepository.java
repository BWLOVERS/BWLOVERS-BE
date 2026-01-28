package com.capstone.bwlovers.insurance.repository;

import com.capstone.bwlovers.insurance.domain.InsuranceProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InsuranceProductRepository extends JpaRepository<InsuranceProduct, Long> {
    Optional<InsuranceProduct> findByUser_UserIdAndResultId(Long userId, String resultId);
}
