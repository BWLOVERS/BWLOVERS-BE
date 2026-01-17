package com.capstone.bwlovers.health.domain;

import com.capstone.bwlovers.health.domain.healthType.PregnancyComplicationType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pregnancy_complications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PregnancyComplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "complication_id")
    private Long complicationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    @Setter(AccessLevel.PACKAGE)
    private HealthStatus healthStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "complication_type", nullable = false)
    private PregnancyComplicationType pregnancyComplicationType;

    void setHealthStatus(HealthStatus healthStatus) {
        this.healthStatus = healthStatus;
    }
}
