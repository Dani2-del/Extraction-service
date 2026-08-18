package com.pruebatecnica.extraction.repository;

import com.pruebatecnica.extraction.entity.ExtractionJobItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExtractionJobItemRepository extends JpaRepository<ExtractionJobItem, Long> {
    List<ExtractionJobItem> findByJobId(String jobId);
}
