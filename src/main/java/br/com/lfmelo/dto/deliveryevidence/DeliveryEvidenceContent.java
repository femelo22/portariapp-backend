package br.com.lfmelo.dto.deliveryevidence;

import br.com.lfmelo.enums.EvidenceContentType;
import org.springframework.core.io.Resource;

public record DeliveryEvidenceContent(Resource resource, EvidenceContentType contentType, long sizeBytes) {
}
