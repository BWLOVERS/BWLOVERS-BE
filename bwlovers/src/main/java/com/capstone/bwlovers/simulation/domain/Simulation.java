package com.capstone.bwlovers.simulation.domain;

import com.capstone.bwlovers.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "simulations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Simulation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "simulation_id")
    private Long id;

    @Column(name = "result_id", nullable = false, unique = true)
    private String resultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "insurance_company", nullable = false)
    private String insuranceCompany;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "question", nullable = false, columnDefinition = "text")
    private String question;

    @Column(name = "result", nullable = false, columnDefinition = "text")
    private String result;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "simulation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SimulationContract> contracts = new ArrayList<>();

    public void addContract(SimulationContract c) {
        c.setSimulation(this);
        this.contracts.add(c);
    }
}