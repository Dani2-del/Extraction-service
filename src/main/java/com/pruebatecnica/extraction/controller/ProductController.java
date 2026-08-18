package com.pruebatecnica.extraction.controller;

import com.pruebatecnica.extraction.dto.ProductRequest;
import com.pruebatecnica.extraction.dto.ProductResponse;
import com.pruebatecnica.extraction.entity.Product;
import com.pruebatecnica.extraction.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    //Crea un nuevo producto manualmente desde la API y responde con 201 Created.
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        Product product = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.fromEntity(product));
    }

    //Obtiene la lista completa de todos los productos almacenados.
    @GetMapping
    public ResponseEntity<List<ProductResponse>> findAll() {
        List<ProductResponse> response = productService.findAll().stream()
                .map(ProductResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    //Busca y devuelve la información de un producto específico por su ID de base de datos.
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ProductResponse.fromEntity(productService.findById(id)));
    }

    //Actualiza parcialmente los datos de un producto existente.
    @PatchMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id, @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ProductResponse.fromEntity(productService.update(id, request)));
    }

    //Elimina un producto de la base de datos y responde con 204 No Content.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
