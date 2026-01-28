package com.capstone.bwlovers.insurance.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "special_contracts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_insurance_contract",
                        columnNames = {"insurance_id", "contract_name", "page_number"})
        },
        indexes = {
                @Index(name = "idx_contract_insurance", columnList = "insurance_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SpecialContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Long contractId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_id", nullable = false)
    @Setter
    private InsuranceProduct insuranceProduct;

    @Column(name = "contract_name", nullable = false)
    private String contractName;

    @Lob
    @Column(name = "contract_description", nullable = false)
    private String contractDescription;

    @Lob
    @Column(name = "key_features", nullable = false)
    private String keyFeatures;

    @Lob
    @Column(name = "contract_recommendation_reason", nullable = false)
    private String contractRecommendationReason;

    @Column(name = "page_number", nullable = false)
    private Long pageNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
