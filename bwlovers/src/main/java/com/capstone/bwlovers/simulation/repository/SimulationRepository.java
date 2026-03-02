package com.capstone.bwlovers.simulation.repository;

import com.capstone.bwlovers.simulation.domain.Simulation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SimulationRepository extends JpaRepository<Simulation, Long> {
    boolean existsByResultId(String resultId);
    Optional<Simulation> findByResultId(String resultId);
    List<Simulation> findByUser_UserIdOrderByCreatedAtAsc(Long userId);
    @EntityGraph(attributePaths = "contracts")
    Optional<Simulation> findByIdAndUser_UserId(Long simulationId, Long userId);
    void deleteByIdAndUser_UserId(Long simulationId, Long userId);
    boolean existsByIdAndUser_UserId(Long simulationId, Long userId);
}
