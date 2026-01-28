package com.capstone.bwlovers.insurance.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "evidence_sources",
        indexes = {
                @Index(name = "idx_evidence_insurance", columnList = "insurance_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EvidenceSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evidence_id")
    private Long evidenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_id", nullable = false)
    @Setter
    private InsuranceProduct insuranceProduct;

    @Column(name = "page_number", nullable = false)
    private Long pageNumber;

    @Lob
    @Column(name = "text_snippet", nullable = false)
    private String textSnippet;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
