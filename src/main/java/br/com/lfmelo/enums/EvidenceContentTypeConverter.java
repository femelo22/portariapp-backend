package br.com.lfmelo.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EvidenceContentTypeConverter implements AttributeConverter<EvidenceContentType, String> {
    @Override
    public String convertToDatabaseColumn(EvidenceContentType attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public EvidenceContentType convertToEntityAttribute(String value) {
        return value == null ? null : EvidenceContentType.fromValue(value);
    }
}
