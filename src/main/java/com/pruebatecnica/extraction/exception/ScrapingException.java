package com.pruebatecnica.extraction.exception;


//excepción personalizada: Sirve para señalar que un elemento solicitado no existe en la base de datos.
public class ScrapingException extends RuntimeException {
    public ScrapingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScrapingException(String message) {
        super(message);
    }
}
