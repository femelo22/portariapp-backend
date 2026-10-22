package br.com.lfmelo.enums;

public enum EvidenceContentType {
    IMAGE_JPEG("image/jpeg"),
    IMAGE_PNG("image/png"),
    IMAGE_WEBP("image/webp"),
    IMAGE_HEIC("image/heic");

    private final String value;

    EvidenceContentType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static EvidenceContentType fromValue(String value) {
        for (var contentType : values()) {
            if (contentType.value.equals(value)) {
                return contentType;
            }
        }
        throw new IllegalArgumentException("Unsupported evidence content type: " + value);
    }
}
