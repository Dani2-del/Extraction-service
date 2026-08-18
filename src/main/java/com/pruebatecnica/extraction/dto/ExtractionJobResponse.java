package com.pruebatecnica.extraction.dto;

import com.pruebatecnica.extraction.entity.ExtractionJob;
import com.pruebatecnica.extraction.entity.JobStatus;
import lombok.Builder;
import lombok.Data;



//Es el DTO de salida que le responde al cliente el estado actual de un 
// trabajo (status) y el conteo en tiempo real de sus productos (total, 
// processed, successful, failed).

@Data
@Builder
public class ExtractionJobResponse {

    private String id;
    private JobStatus status;
    private int total;
    private int processed;
    private int successful;
    private int failed;

    public static ExtractionJobResponse fromEntity(ExtractionJob job) {
        return ExtractionJobResponse.builder()
                .id(job.getId())
                .status(job.getStatus())
                .total(job.getTotal())
                .processed(job.getProcessed())
                .successful(job.getSuccessful())
                .failed(job.getFailed())
                .build();
    }
}
