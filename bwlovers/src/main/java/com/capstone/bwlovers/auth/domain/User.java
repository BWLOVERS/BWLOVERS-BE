package com.capstone.bwlovers.auth.domain;

import com.capstone.bwlovers.health.domain.HealthStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users",
        uniqueConstraints = {@UniqueConstraint(
                name = "uk_provider_providerId",
                columnNames = {"provider", "provider_id"}
                )
        }
)
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
    private String providerId;

    @Column(length = 100)
    private String email;

    @Column(length = 50)
    private String username; // 네이버 닉네임

    @Column(length = 20)
    private String phone;

//    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
//    private PregnancyInfo pregnancyInfo;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private HealthStatus healthStatus;

    public void setHealthStatus(HealthStatus healthStatus) {
        this.healthStatus = healthStatus;
        if (healthStatus != null && healthStatus.getUser() != this) {
            healthStatus.setUser(this);
        }
    }

}
