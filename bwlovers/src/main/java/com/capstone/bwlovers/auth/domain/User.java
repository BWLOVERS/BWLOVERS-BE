package com.capstone.bwlovers.auth.domain;

import com.capstone.bwlovers.maternity.domain.HealthStatus;
import com.capstone.bwlovers.maternity.domain.PregnancyInfo;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users", uniqueConstraints = {@UniqueConstraint(name = "uk_provider_providerId", columnNames = {"provider", "provider_id"})})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OAuthProvider provider; // NAVER

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId; // 네이버 고유 ID

    @Column(length = 100)
    private String email;

    @Column(length = 50)
    private String username; // 네이버 닉네임

    @Column(length = 20)
    private String phone;

    // 1:1 - 임신 기본 정보
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private PregnancyInfo pregnancyInfo;

    // 1:1 - 건강 상태 정보
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private HealthStatus healthStatus;

    // 연관관계 편의 메서드
    public void setPregnancyInfo(PregnancyInfo pregnancyInfo) {
        this.pregnancyInfo = pregnancyInfo;
        if (pregnancyInfo != null) {
            pregnancyInfo.setUser(this);
        }
    }

    public void setHealthStatus(HealthStatus healthStatus) {
        this.healthStatus = healthStatus;
        if (healthStatus != null) {
            healthStatus.setUser(this);
        }
    }
}
