package com.pruebatecnica.extraction.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "extraction_jobs")
@Getter
@Setter
@NoArgsConstructor
public class ExtractionJob {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    private int total;
    private int processed;
    private int successful;
    private int failed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static ExtractionJob createNew(int total) {
        ExtractionJob job = new ExtractionJob();
        job.id = UUID.randomUUID().toString();
        job.status = JobStatus.PENDING;
        job.total = total;
        job.processed = 0;
        job.successful = 0;
        job.failed = 0;
        LocalDateTime now = LocalDateTime.now();
        job.createdAt = now;
        job.updatedAt = now;
        return job;
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
