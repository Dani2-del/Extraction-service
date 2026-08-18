package com.pruebatecnica.extraction.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

//Es el DTO de entrada utilizado para crear o
//  actualizar productos manualmente desde la
//  API, exigiendo con @NotBlank que el campo del
//  nombre no venga vacío.

@Data
public class ProductRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    private BigDecimal price;
    private String category;
    private String availability;
    private String condition;
    private String brand;
    private String sourceUrl;
    private String externalId;
}
