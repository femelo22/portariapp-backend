package br.com.lfmelo.storage;

public class EvidenceSizeExceededException extends RuntimeException {

    public EvidenceSizeExceededException() {
        super("Evidence file exceeds the configured limit");
    }
}
