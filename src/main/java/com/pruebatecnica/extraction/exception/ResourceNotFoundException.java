package com.pruebatecnica.extraction.exception;

//Representa fallos específicos al intentar raspar o procesar la web externa (caídas de red, timeouts o cambios en el HTML).
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
