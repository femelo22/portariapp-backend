package br.com.lfmelo.exception;

/** Thrown when a request violates a business uniqueness/consistency rule. Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
