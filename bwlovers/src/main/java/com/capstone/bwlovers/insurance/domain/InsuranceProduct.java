package com.capstone.bwlovers.insurance.domain;

import com.capstone.bwlovers.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "insurance_products",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_result", columnNames = {"user_id", "result_id"})
        },
        indexes = {
                @Index(name = "idx_insurance_user_created", columnList = "user_id, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InsuranceProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "insurance_id")
    private Long insuranceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "result_id", nullable = false, length = 64)
    private String resultId;

    @Column(name = "insurance_company", nullable = false)
    private String insuranceCompany;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "is_long_term", nullable = false)
    private boolean isLongTerm;

    @Column(name = "monthly_cost", nullable = false)
    private Long monthlyCost;

    @Lob
    @Column(name = "insurance_recommendation_reason", nullable = false)
    private String insuranceRecommendationReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "insuranceProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SpecialContract> specialContracts = new ArrayList<>();

    @OneToMany(mappedBy = "insuranceProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EvidenceSource> evidenceSources = new ArrayList<>();

    public void addContract(SpecialContract contract) {
        specialContracts.add(contract);
        contract.setInsuranceProduct(this);
    }

    public void addEvidence(EvidenceSource evidence) {
        evidenceSources.add(evidence);
        evidence.setInsuranceProduct(this);
    }
}
