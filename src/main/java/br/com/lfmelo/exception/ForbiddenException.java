package br.com.lfmelo.exception;

/** Thrown when an authenticated staff member tries to access a resource outside their condominium. Mapped to HTTP 403. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
