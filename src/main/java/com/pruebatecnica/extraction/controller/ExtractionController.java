package com.pruebatecnica.extraction.controller;

import com.pruebatecnica.extraction.dto.ExtractionJobResponse;
import com.pruebatecnica.extraction.dto.ExtractionRequest;
import com.pruebatecnica.extraction.dto.ProductResponse;
import com.pruebatecnica.extraction.entity.ExtractionJob;
import com.pruebatecnica.extraction.service.ExtractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/extractions")
@RequiredArgsConstructor
public class ExtractionController {

    private final ExtractionService extractionService;

    /**
     * Crea un trabajo de extraccion y dispara el procesamiento asincrono.
     * Responde 202 Accepted de inmediato, sin esperar a que termine.
     */

    //Recibe la lista de IDs a extraer, manda a crear el trabajo y responde de inmediato con un estado 202 Accepted junto al DTO
    @PostMapping
    public ResponseEntity<ExtractionJobResponse> create(@Valid @RequestBody ExtractionRequest request) {
        ExtractionJob job = extractionService.createJob(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ExtractionJobResponse.fromEntity(job));
    }

    //Recibe el ID del trabajo y devuelve el progreso actualizado en tiempo real (estado actual, cuántos procesados, exitosos y fallidos).
    @GetMapping("/{id}")
    public ResponseEntity<ExtractionJobResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(ExtractionJobResponse.fromEntity(extractionService.findById(id)));
    }

    /**
     * Productos obtenidos exitosamente como resultado de este job.
     */
    //Retorna la lista con los detalles completos de todos los productos (ProductResponse) que fueron extraídos con éxito por ese trabajo en particular.
    @GetMapping("/{id}/products")
    public ResponseEntity<List<ProductResponse>> resultingProducts(@PathVariable String id) {
        List<ProductResponse> response = extractionService.findResultingProducts(id).stream()
                .map(ProductResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }
}
