package com.capstone.bwlovers.user.domain;

import com.capstone.bwlovers.maternity.domain.HealthStatus;
import com.capstone.bwlovers.maternity.domain.PregnancyInfo;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true, length = 50)
    private String loginId;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 100)
    private String email;

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