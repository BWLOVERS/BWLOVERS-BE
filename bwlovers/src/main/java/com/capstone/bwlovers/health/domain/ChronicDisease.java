package com.capstone.bwlovers.health.domain;

import com.capstone.bwlovers.health.domain.healthType.ChronicDiseaseType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chronic_diseases")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChronicDisease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chronic_id")
    private Long chronicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    @Setter(AccessLevel.PACKAGE)
    private HealthStatus healthStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "chronic_disease_type", nullable = false)
    private ChronicDiseaseType chronicDiseaseType;

    @Column(name = "chronic_on_medication", nullable = false)
    private boolean chronicOnMedication;

    void setHealthStatus(HealthStatus healthStatus) {
        this.healthStatus = healthStatus;
    }
}
