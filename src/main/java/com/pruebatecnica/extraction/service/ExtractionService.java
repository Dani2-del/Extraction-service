package com.pruebatecnica.extraction.service;

import com.pruebatecnica.extraction.dto.ExtractionRequest;
import com.pruebatecnica.extraction.entity.ExtractionJob;
import com.pruebatecnica.extraction.entity.ExtractionJobItem;
import com.pruebatecnica.extraction.entity.ItemStatus;
import com.pruebatecnica.extraction.entity.Product;
import com.pruebatecnica.extraction.exception.ResourceNotFoundException;
import com.pruebatecnica.extraction.repository.ExtractionJobItemRepository;
import com.pruebatecnica.extraction.repository.ExtractionJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExtractionService {

    private final ExtractionJobRepository extractionJobRepository;
    private final ExtractionJobItemRepository extractionJobItemRepository;
    private final ExtractionAsyncService extractionAsyncService;
    private final ProductService productService;

    /**
     * Crea el job en estado PENDING, lo persiste y dispara el
     * procesamiento asincrono. Devuelve de inmediato sin esperar
     * a que termine el procesamiento.
     */
    
    public ExtractionJob createJob(ExtractionRequest request) {
        ExtractionJob job = ExtractionJob.createNew(request.getProductIds().size());
        extractionJobRepository.save(job);

        // Se dispara despues de crear el job; al ser un bean distinto,
        // Spring aplica correctamente el proxy @Async (evita el problema
        // de self-invocation).
        extractionAsyncService.processJob(job.getId(), request.getProductIds());

        return job;
    }

    @Transactional(readOnly = true)
    public ExtractionJob findById(String id) {
        return extractionJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job de extraccion no encontrado: " + id));
    }

    /**
     * Devuelve los productos que resultaron exitosamente extraidos por este job.
     */
    @Transactional(readOnly = true)
    public List<Product> findResultingProducts(String jobId) {
        findById(jobId); // valida existencia del job (lanza 404 si no existe)
        List<String> successfulExternalIds = extractionJobItemRepository.findByJobId(jobId).stream()
                .filter(item -> item.getStatus() == ItemStatus.SUCCESS)
                .map(ExtractionJobItem::getExternalProductId)
                .toList();
        return productService.findByExternalIds(successfulExternalIds);
    }
}
