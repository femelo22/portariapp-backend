package br.com.lfmelo.exception;

/** Thrown when authentication is missing or invalid (e.g. bad credentials/token). Mapped to HTTP 401. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
