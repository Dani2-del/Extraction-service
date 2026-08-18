package com.pruebatecnica.extraction.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

//ExtractionRequest: Es el DTO de entrada para solicitar una nuevaç
//  extracción; usa @NotEmpty para garantizar que la API 
// rechace la petición si no envían al menos un ID de producto.

@Data
public class ExtractionRequest {

    @NotEmpty(message = "Debe indicar al menos un productId")
    private List<Long> productIds;
}
