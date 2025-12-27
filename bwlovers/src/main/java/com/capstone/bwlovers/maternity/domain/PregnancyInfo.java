package com.capstone.bwlovers.maternity.domain;

import com.capstone.bwlovers.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "pregnancy_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PregnancyInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long infoId;

    // User 와 1:1 관계 (FK + UNIQUE)
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Integer age;

    @Column(nullable = false)
    private Integer height;

    @Column(nullable = false)
    private Integer weightPre; // 임신 전 몸무게(kg)

    @Column(nullable = false)
    private Integer weightCurrent; // 현재 몸무게 (kg)

    @Column(nullable = false)
    private Boolean isFirstbirth; // 초산 여부

    @Column(nullable = false)
    private Integer gestationalWeek; // 임신 주차

    @Column(nullable = false)
    private LocalDate expectedDate; // 출산 예정일

    @Column(nullable = false)
    private Boolean isMultiplePregnancy; // 쌍둥이/다태 임신 여부

    @Column
    private Integer miscarriageHistory;  // 유산 이력(횟수), 없으면 null

    // 연관관계 편의 메서드
    public void setUser(User user) {
        this.user = user;
    }
}
