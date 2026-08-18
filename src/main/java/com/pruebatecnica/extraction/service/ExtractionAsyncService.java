package com.pruebatecnica.extraction.service;

import com.pruebatecnica.extraction.entity.ExtractionJob;
import com.pruebatecnica.extraction.entity.ExtractionJobItem;
import com.pruebatecnica.extraction.entity.ItemStatus;
import com.pruebatecnica.extraction.entity.JobStatus;
import com.pruebatecnica.extraction.exception.ScrapingException;
import com.pruebatecnica.extraction.repository.ExtractionJobItemRepository;
import com.pruebatecnica.extraction.repository.ExtractionJobRepository;
import com.pruebatecnica.extraction.service.scraper.AutomationExerciseScraper;
import com.pruebatecnica.extraction.service.scraper.ScrapedProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * NOTA sobre transacciones: los metodos internos de esta clase NO llevan
 * @Transactional propio porque se invocan entre si via "this" (auto-invocacion),
 * lo cual Spring no intercepta con su proxy y la anotacion seria ignorada
 * silenciosamente. En su lugar, cada operacion de persistencia se apoya en
 * que JpaRepository.save()/findById() ya son transaccionales por si mismos
 * (SimpleJpaRepository esta anotado con @Transactional a nivel de metodo).
 * Esto es suficiente aqui porque cada actualizacion de job/item es atomica
 * por si sola; si se necesitara atomicidad entre varias escrituras, esa
 * logica deberia extraerse a otro bean para que el proxy de Spring aplique.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractionAsyncService {

    private final ExtractionJobRepository extractionJobRepository;
    private final ExtractionJobItemRepository extractionJobItemRepository;
    private final AutomationExerciseScraper scraper;
    private final ProductService productService;

    //Procesa y extrae la información producto por producto, de forma individual utilizando su ID.
    @Async("extractionExecutor")
    public void processJob(String jobId, List<Long> productIds) {
        markProcessing(jobId);

        for (Long productId : productIds) {
            processSingleProduct(jobId, productId);
        }

        finalizeJob(jobId);
    }

    //Cambia el estado del job a "PROCESSING" y actualiza la fecha de modificación.
    private void markProcessing(String jobId) {
        ExtractionJob job = extractionJobRepository.findById(jobId).orElseThrow();
        job.setStatus(JobStatus.PROCESSING);
        job.touch();
        extractionJobRepository.save(job);
    }

    //Procesa un solo producto, actualizando el estado del item y del job según el resultado.
    //ejecuta la extracción web de un único producto, maneja los posibles
    //  fallos sin detener el lote y guarda el progreso en la base de datos.
    private void processSingleProduct(String jobId, Long productId) {
        ExtractionJob job = extractionJobRepository.findById(jobId).orElseThrow();
        ExtractionJobItem item = new ExtractionJobItem(job, String.valueOf(productId));

        try {
            ScrapedProduct scraped = scraper.fetchProduct(String.valueOf(productId));
            productService.upsertFromScraping(scraped);

            item.setStatus(ItemStatus.SUCCESS);
            job.setSuccessful(job.getSuccessful() + 1);
        } catch (ScrapingException e) {
            log.warn("Fallo al extraer producto {} del job {}: {}", productId, jobId, e.getMessage());
            item.setStatus(ItemStatus.FAILED);
            item.setErrorMessage(e.getMessage());
            job.setFailed(job.getFailed() + 1);
        } catch (Exception e) {
            log.error("Error inesperado al extraer producto {} del job {}", productId, jobId, e);
            item.setStatus(ItemStatus.FAILED);
            item.setErrorMessage("Error inesperado: " + e.getMessage());
            job.setFailed(job.getFailed() + 1);
        }

        job.setProcessed(job.getProcessed() + 1);
        job.touch();

        extractionJobItemRepository.save(item);
        extractionJobRepository.save(job);
    }

    //evalúa cómo le fue a todo el lote y guarda el estado definitivo del trabajo en la base de datos.
    private void finalizeJob(String jobId) {
        ExtractionJob job = extractionJobRepository.findById(jobId).orElseThrow();

        if (job.getFailed() == 0) {
            job.setStatus(JobStatus.COMPLETED);
        } else if (job.getSuccessful() == 0) {
            job.setStatus(JobStatus.FAILED);
        } else {
            job.setStatus(JobStatus.COMPLETED_WITH_ERRORS);
        }
        job.touch();
        extractionJobRepository.save(job);
    }
}
