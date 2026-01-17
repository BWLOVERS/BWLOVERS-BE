package com.capstone.bwlovers.pregnancy.domain;

import com.capstone.bwlovers.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "pregnancy_info",
        uniqueConstraints = @UniqueConstraint(name = "uk_pregnancy_info_user", columnNames = "user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PregnancyInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long infoId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false)
    private Integer height;

    @Column(name = "weight_pre", nullable = false)
    private Integer weightPre;

    @Column(name = "weight_current", nullable = false)
    private Integer weightCurrent;

    @Column(name = "is_firstbirth", nullable = false)
    private Boolean isFirstbirth;

    @Column(name = "gestational_week", nullable = false)
    private Integer gestationalWeek;

    @Column(name = "expected_date", nullable = false)
    private LocalDate expectedDate;

    @Column(name = "is_multiple_pregnancy", nullable = false)
    private Boolean isMultiplePregnancy;

    @Column(name = "miscarriage_history", nullable = false)
    private Integer miscarriageHistory;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "pregnancy_info_jobs",
            joinColumns = @JoinColumn(name = "info_id"),
            inverseJoinColumns = @JoinColumn(name = "job_id")
    )
    private java.util.Set<Job> jobs = new java.util.LinkedHashSet<>();

    public void clearJobs() {
        this.jobs.clear();
    }

    public void addJob(Job job) {
        this.jobs.add(job);
    }

    public void update(
            LocalDate birthDate,
            Integer height,
            Integer weightPre,
            Integer weightCurrent,
            Boolean isFirstbirth,
            Integer gestationalWeek,
            LocalDate expectedDate,
            Boolean isMultiplePregnancy,
            Integer miscarriageHistory
    ) {
        this.birthDate = birthDate;
        this.height = height;
        this.weightPre = weightPre;
        this.weightCurrent = weightCurrent;
        this.isFirstbirth = isFirstbirth;
        this.gestationalWeek = gestationalWeek;
        this.expectedDate = expectedDate;
        this.isMultiplePregnancy = isMultiplePregnancy;
        this.miscarriageHistory = miscarriageHistory;
    }
}
