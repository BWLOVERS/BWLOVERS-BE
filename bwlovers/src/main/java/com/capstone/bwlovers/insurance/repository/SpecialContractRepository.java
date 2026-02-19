package com.capstone.bwlovers.insurance.repository;

import com.capstone.bwlovers.insurance.domain.SpecialContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecialContractRepository extends JpaRepository<SpecialContract, Long> {
    List<SpecialContract> findAllByInsuranceProduct_InsuranceIdAndContractIdIn(Long insuranceId, List<Long> contractIds);
}
