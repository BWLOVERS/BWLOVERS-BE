package com.capstone.bwlovers.pregnancy.repository;

import com.capstone.bwlovers.pregnancy.domain.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    Optional<Job> findByJobName(String jobName);
}
