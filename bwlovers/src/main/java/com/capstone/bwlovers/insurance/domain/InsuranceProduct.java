package com.capstone.bwlovers.insurance.domain;

import com.capstone.bwlovers.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "insurance_products")
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

    @Column(name = "result_id", nullable = false)
    private String resultId;

    @Column(name = "item_id", nullable = false)
    private String itemId;

    @Column(name = "insurance_company", nullable = false)
    private String insuranceCompany;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "is_long_term", nullable = false)
    private boolean isLongTerm;

    @Column(name = "monthly_cost", nullable = false)
    private Long monthlyCost;

    @Column(name = "insurance_recommendation_reason", columnDefinition = "text")
    private String insuranceRecommendationReason;

    @Column(name = "memo", nullable = true, columnDefinition = "text")
    private String memo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "insuranceProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SpecialContract> specialContracts = new ArrayList<>();

    public void addContract(SpecialContract c) {
        c.setInsuranceProduct(this);
        this.specialContracts.add(c);
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }
}