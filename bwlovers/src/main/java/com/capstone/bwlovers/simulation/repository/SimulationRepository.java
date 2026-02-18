package com.capstone.bwlovers.simulation.repository;

import com.capstone.bwlovers.simulation.domain.Simulation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SimulationRepository extends JpaRepository<Simulation, Long> {
    boolean existsByResultId(String resultId);
    Optional<Simulation> findByResultId(String resultId);
}
