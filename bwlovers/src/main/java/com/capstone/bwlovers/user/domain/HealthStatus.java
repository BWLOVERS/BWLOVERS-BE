package com.capstone.bwlovers.user.domain;

import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.user.domain.healthType.ChronicDiseaseType;
import com.capstone.bwlovers.user.domain.healthType.PastDiseaseType;
import com.capstone.bwlovers.user.domain.healthType.PregnancyComplicationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class HealthStatus {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // 과거 병력
    @Column(name = "past_cured")
    private Boolean pastCured;

    @Column(name = "past_last_treated_at")
    private LocalDate pastLastTreatedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "health_status_past_diseases", joinColumns = @JoinColumn(name = "health_status_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "past_disease_type")
    private Set<PastDiseaseType> pastDiseases = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "health_status_chronic_diseases", joinColumns = @JoinColumn(name = "health_status_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "chronic_disease_type")
    private Set<ChronicDiseaseType> chronicDiseases = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "health_status_pregnancy_complications", joinColumns = @JoinColumn(name = "health_status_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "pregnancy_complication_type")
    private Set<PregnancyComplicationType> pregnancyComplications = new HashSet<>();

    public void setUser(User user) {
        this.user = user;
    }

    public void updateHealthStatus(Boolean pastCured,
                            LocalDate pastLastTreatedAt,
                            Set<PastDiseaseType> pastDiseases,
                            Set<ChronicDiseaseType> chronicDiseases,
                            Set<PregnancyComplicationType> pregnancyComplications) {
        this.pastCured = pastCured;
        this.pastLastTreatedAt = pastLastTreatedAt;
        this.pastDiseases = (pastDiseases == null) ? new HashSet<>() : new HashSet<>(pastDiseases);
        this.chronicDiseases = (chronicDiseases == null) ? new HashSet<>() : new HashSet<>(chronicDiseases);
        this.pregnancyComplications = (pregnancyComplications == null) ? new HashSet<>() : new HashSet<>(pregnancyComplications);
        this.updatedAt = LocalDateTime.now();
    }
}
