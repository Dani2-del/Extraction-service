package com.pruebatecnica.extraction.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "extraction_job_items")
@Getter
@Setter
@NoArgsConstructor
public class ExtractionJobItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private ExtractionJob job;

    @Column(name = "external_product_id", nullable = false)
    private String externalProductId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemStatus status;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public ExtractionJobItem(ExtractionJob job, String externalProductId) {
        this.job = job;
        this.externalProductId = externalProductId;
        this.status = ItemStatus.PENDING;
    }
}
