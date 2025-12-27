package com.capstone.bwlovers.maternity.domain;

import com.capstone.bwlovers.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "health_status")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class HealthStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long statusId;

    // User 와 1:1 관계 (FK + UNIQUE)
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * 과거 병력
     * 예: ["고혈압", "제2형 당뇨병", "갑상선 기능 저하증"]
     * JSON 문자열로 저장 (PostgreSQL jsonb)
     */
    @Column(columnDefinition = "jsonb")
    private String pastHistory;

    /**
     * 현재 복용 약물 정보
     * 예: [{"name": "levothyroxine", "dose": "50mcg", "freq": "qd"}]
     */
    @Column(columnDefinition = "jsonb")
    private String medicine;

    /**
     * 현재 건강 상태 (자유 서술)
     * 예: "가끔 두통, 심한 피로감, 부종 약간"
     */
    @Column(columnDefinition = "text")
    private String currentCondition;

    /**
     * 만성질환(Chronic conditions)
     * 예: ["고혈압", "당뇨병", "천식"]
     */
    @Column(columnDefinition = "jsonb")
    private String chronicConditions;

    /**
     * 임신 합병증(pregnancy complications)
     * 예: ["임신성 당뇨", "전치태반"] 또는
     * {"gestational_diabetes": true, "placenta_previa": true}
     */
    @Column(columnDefinition = "jsonb")
    private String pregnancyComplications;

    // 연관관계 편의 메서드
    public void setUser(User user) {
        this.user = user;
    }
}
