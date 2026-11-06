package br.com.lfmelo.storage;

import java.io.InputStream;
import br.com.lfmelo.enums.EvidenceContentType;
import org.springframework.core.io.Resource;

public interface EvidenceStorage {
    record StoredEvidence(String key, EvidenceContentType contentType, long sizeBytes) {
    }

    StoredEvidence store(InputStream input, EvidenceContentType declaredType, long maximumBytes);

    Resource load(String key);

    void delete(String key);
}
