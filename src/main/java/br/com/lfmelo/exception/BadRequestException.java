package br.com.lfmelo.exception;

/** Thrown when a request is semantically invalid (e.g. path/body mismatch). Mapped to HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
