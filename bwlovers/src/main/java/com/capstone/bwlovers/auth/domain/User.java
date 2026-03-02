package com.capstone.bwlovers.auth.domain;

import com.capstone.bwlovers.auth.dto.response.UserInfoResponse;
import com.capstone.bwlovers.health.domain.HealthStatus;
import com.capstone.bwlovers.pregnancy.domain.PregnancyInfo;
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

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Column(name = "naver_access_token", length = 500) // 네이버 연동 해제용
    private String naverAccessToken;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private PregnancyInfo pregnancyInfo;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private HealthStatus healthStatus;

    public void updateNaverToken(String naverAccessToken) {
        this.naverAccessToken = naverAccessToken;
    }

    public void updateUsername(String username) {
        this.username = username;
    }

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void clearRelations() {
        if (this.pregnancyInfo != null) {
            this.pregnancyInfo.setUser(null);
            this.pregnancyInfo = null;
        }
        if (this.healthStatus != null) {
            this.healthStatus.setUser(null);
            this.healthStatus = null;
        }
    }

}
