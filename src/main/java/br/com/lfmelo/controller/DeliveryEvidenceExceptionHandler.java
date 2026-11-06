package br.com.lfmelo.controller;

import br.com.lfmelo.exception.ErrorResponse;
import br.com.lfmelo.storage.EvidenceStorageException;
import br.com.lfmelo.storage.EvidenceSizeExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Order(-1)
@RestControllerAdvice
public class DeliveryEvidenceExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(DeliveryEvidenceExceptionHandler.class);

    @ExceptionHandler(EvidenceStorageException.class)
    public ResponseEntity<ErrorResponse> storage(EvidenceStorageException ex, WebRequest request) {
        log.error("Evidence storage operation failed", ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Evidence storage operation failed", request);
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class, EvidenceSizeExceededException.class})
    public ResponseEntity<ErrorResponse> size(Exception ex, WebRequest request) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "Evidence file exceeds the configured limit", request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> missing(MissingServletRequestPartException ex, WebRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Multipart file is required", request);
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String message, WebRequest request) {
        return ResponseEntity.status(status).body(ErrorResponse.of(
                status.value(), status.getReasonPhrase(), message, request.getDescription(false).replace("uri=", "")));
    }
}
