package com.pruebatecnica.extraction.service.scraper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class ScrapedProduct {
    private String externalId;
    private String name;
    private BigDecimal price;
    private String category;
    private String availability;
    private String condition;
    private String brand;
    private String sourceUrl;
}
