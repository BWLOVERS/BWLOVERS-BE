package com.capstone.bwlovers.health.domain;

import com.capstone.bwlovers.health.domain.healthType.PastDiseaseType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "past_diseases")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PastDisease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "past_id")
    private Long pastId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    @Setter(AccessLevel.PACKAGE)
    private HealthStatus healthStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "past_disease_type", nullable = false)
    private PastDiseaseType pastDiseaseType;

    @Column(name = "past_cured", nullable = false)
    private boolean pastCured;

    @Column(name = "past_last_treated_at", nullable = true)
    private LocalDate pastLastTreatedAt;

    void setHealthStatus(HealthStatus healthStatus) {
        this.healthStatus = healthStatus;
    }
}
