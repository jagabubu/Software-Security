package com.northbridge.casevault.model;

import java.util.Optional;

public enum EvidenceType {
    TXT("txt", "text/plain"),
    LOG("log", "text/plain"),
    CSV("csv", "text/csv"),
    PDF("pdf", "application/pdf"),
    PNG("png", "image/png");

    private final String extension;
    private final String contentType;

    EvidenceType(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String getExtension() { return extension; }
    public String getContentType() { return contentType; }

    public static Optional<EvidenceType> fromExtension(String extension) {
        for (EvidenceType type : values()) {
            if (type.extension.equals(extension)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}