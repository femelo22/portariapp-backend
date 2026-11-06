package br.com.lfmelo.storage;

public class EvidenceStorageException extends RuntimeException {
    public EvidenceStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public EvidenceStorageException(String message) {
        super(message);
    }
}
