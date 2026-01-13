package com.capstone.bwlovers.health.domain;

import com.capstone.bwlovers.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "health_status")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HealthStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Long statusId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @Setter
    private User user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "healthStatus", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PastDisease> pastDiseases = new ArrayList<>();

    @OneToMany(mappedBy = "healthStatus", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChronicDisease> chronicDiseases = new ArrayList<>();

    @OneToMany(mappedBy = "healthStatus", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PregnancyComplication> pregnancyComplications = new ArrayList<>();

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addPastDisease(PastDisease pd) {
        pastDiseases.add(pd);
        pd.setHealthStatus(this);
    }

    public void addChronicDisease(ChronicDisease cd) {
        chronicDiseases.add(cd);
        cd.setHealthStatus(this);
    }

    public void addPregnancyComplication(PregnancyComplication pc) {
        pregnancyComplications.add(pc);
        pc.setHealthStatus(this);
    }

    public void clearChildren() {
        pastDiseases.clear();
        chronicDiseases.clear();
        pregnancyComplications.clear();
    }
}
