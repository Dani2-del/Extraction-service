package com.pruebatecnica.extraction.dto;

import com.pruebatecnica.extraction.entity.Product;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductResponse {

    private Long id;
    private String externalId;
    private String name;
    private BigDecimal price;
    private String category;
    private String availability;
    private String condition;
    private String brand;
    private String sourceUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponse fromEntity(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .externalId(p.getExternalId())
                .name(p.getName())
                .price(p.getPrice())
                .category(p.getCategory())
                .availability(p.getAvailability())
                .condition(p.getCondition())
                .brand(p.getBrand())
                .sourceUrl(p.getSourceUrl())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
