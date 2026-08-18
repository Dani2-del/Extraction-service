package com.pruebatecnica.extraction.service;

import com.pruebatecnica.extraction.dto.ProductRequest;
import com.pruebatecnica.extraction.entity.Product;
import com.pruebatecnica.extraction.exception.ResourceNotFoundException;
import com.pruebatecnica.extraction.repository.ProductRepository;
import com.pruebatecnica.extraction.service.scraper.ScrapedProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;


    //Crea y guarda un producto nuevo manualmente.
    @Transactional
    public Product create(ProductRequest request) {
        Product product = Product.builder()
                .externalId(request.getExternalId())
                .name(request.getName())
                .price(request.getPrice())
                .category(request.getCategory())
                .availability(request.getAvailability())
                .condition(request.getCondition())
                .brand(request.getBrand())
                .sourceUrl(request.getSourceUrl())
                .build();
        return productRepository.save(product);
    }

    //Devuelve todos los productos registrados en el sistema.
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    //Busca un producto por su ID interno (la clave primaria de la base de datos).
    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }

    //Busca múltiples productos a partir de sus IDs externos (los números de producto del sitio web
    @Transactional(readOnly = true)
    public List<Product> findByExternalIds(List<String> externalIds) {
        return externalIds.stream()
                .map(productRepository::findByExternalId)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();
    }

    //Actualiza parcialmente los datos de un producto existente
    @Transactional
    public Product update(Long id, ProductRequest request) {
        Product product = findById(id);
        if (request.getName() != null) product.setName(request.getName());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getCategory() != null) product.setCategory(request.getCategory());
        if (request.getAvailability() != null) product.setAvailability(request.getAvailability());
        if (request.getCondition() != null) product.setCondition(request.getCondition());
        if (request.getBrand() != null) product.setBrand(request.getBrand());
        if (request.getSourceUrl() != null) product.setSourceUrl(request.getSourceUrl());
        if (request.getExternalId() != null) product.setExternalId(request.getExternalId());
        return productRepository.save(product);
    }

    //Elimina un producto de la base de datos.
    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Producto no encontrado: " + id);
        }
        productRepository.deleteById(id);
    }

    /**
     * Inserta o actualiza (upsert) un producto obtenido por scraping,
     * usando el externalId como clave de deduplicacion.
     */
    @Transactional
    public Product upsertFromScraping(ScrapedProduct scraped) {
        Product product = productRepository.findByExternalId(scraped.getExternalId())
                .orElseGet(Product::new);

        product.setExternalId(scraped.getExternalId());
        product.setName(scraped.getName());
        product.setPrice(scraped.getPrice());
        product.setCategory(scraped.getCategory());
        product.setAvailability(scraped.getAvailability());
        product.setCondition(scraped.getCondition());
        product.setBrand(scraped.getBrand());
        product.setSourceUrl(scraped.getSourceUrl());

        return productRepository.save(product);
    }
}
