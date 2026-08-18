package com.pruebatecnica.extraction.repository;

import com.pruebatecnica.extraction.entity.ExtractionJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExtractionJobRepository extends JpaRepository<ExtractionJob, String> {
}
