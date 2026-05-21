package com.capstone.bwlovers.insurance.repository;

import com.capstone.bwlovers.insurance.domain.InsuranceProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InsuranceProductRepository extends JpaRepository<InsuranceProduct, Long> {
    Optional<InsuranceProduct> findByUser_UserIdAndResultIdAndItemId(Long userId, String resultId, String itemId);
    Optional<InsuranceProduct> findTopByInsuranceCompanyAndProductNameOrderByCreatedAtDesc(String insuranceCompany, String productName);
    List<InsuranceProduct> findAllByUser_UserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT DISTINCT p FROM InsuranceProduct p LEFT JOIN FETCH p.specialContracts WHERE p.user.userId = :userId ORDER BY p.createdAt DESC")
    List<InsuranceProduct> findAllByUserIdWithContracts(@Param("userId") Long userId);

    @Query("SELECT DISTINCT p FROM InsuranceProduct p LEFT JOIN FETCH p.specialContracts WHERE p.insuranceId = :insuranceId")
    Optional<InsuranceProduct> findByInsuranceIdWithContracts(@Param("insuranceId") Long insuranceId);
}
