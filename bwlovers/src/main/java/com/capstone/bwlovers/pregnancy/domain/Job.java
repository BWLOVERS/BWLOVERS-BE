package com.capstone.bwlovers.pregnancy.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long jobId;

    @Column(name = "job_name", nullable = false, length = 100, unique = true)
    private String jobName;

    @Column(name = "risk_level", nullable = false)
    private Integer riskLevel; // 1~5

}
