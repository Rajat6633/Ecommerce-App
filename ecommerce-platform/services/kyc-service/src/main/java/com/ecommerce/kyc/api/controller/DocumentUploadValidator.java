package com.ecommerce.kyc.api.controller;

import com.ecommerce.kyc.api.exception.InvalidDocumentUploadException;

import java.util.Set;

/**
 * Validates an uploaded ID-document image before it reaches the extraction
 * pipeline. Enforces a strict content-type allow-list, verifies the declared
 * type against the file's magic bytes (so a renamed/forged file is rejected),
 * and bounds the size. All failures surface as
 * {@link InvalidDocumentUploadException} (HTTP 400), never a 500.
 */
final class DocumentUploadValidator {

    static final String PNG = "image/png";
    static final String JPEG = "image/jpeg";
    static final String WEBP = "image/webp";
    static final Set<String> ALLOWED_TYPES = Set.of(PNG, JPEG, WEBP);

    /** Mirrors spring.servlet.multipart.max-file-size (8MB). */
    static final long MAX_BYTES = 8L * 1024 * 1024;

    private DocumentUploadValidator() {
    }

    /**
     * @return the validated, allow-listed content type (safe to hand to a MIME parser).
     * @throws InvalidDocumentUploadException on any validation failure.
     */
    static String validate(byte[] bytes, String declaredContentType) {
        if (bytes == null || bytes.length == 0) {
            throw new InvalidDocumentUploadException("Uploaded document is empty");
        }
        if (bytes.length > MAX_BYTES) {
            throw new InvalidDocumentUploadException(
                    "Uploaded document exceeds the " + MAX_BYTES + "-byte limit");
        }
        String type = declaredContentType == null ? "" : declaredContentType.trim().toLowerCase(java.util.Locale.ROOT);
        if (!ALLOWED_TYPES.contains(type)) {
            throw new InvalidDocumentUploadException(
                    "Unsupported content type '" + declaredContentType + "'; allowed: " + ALLOWED_TYPES);
        }
        if (!magicBytesMatch(bytes, type)) {
            throw new InvalidDocumentUploadException(
                    "File content does not match declared type '" + type + "'");
        }
        return type;
    }

    private static boolean magicBytesMatch(byte[] b, String type) {
        return switch (type) {
            case PNG -> startsWith(b, 0x89, 0x50, 0x4E, 0x47); // .PNG
            case JPEG -> startsWith(b, 0xFF, 0xD8, 0xFF);       // JPEG SOI
            case WEBP -> isWebp(b);                             // RIFF....WEBP
            default -> false;
        };
    }

    private static boolean isWebp(byte[] b) {
        // 'R''I''F''F' [4-byte size] 'W''E''B''P'
        return b.length >= 12
                && startsWith(b, 'R', 'I', 'F', 'F')
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
    }

    private static boolean startsWith(byte[] b, int... prefix) {
        if (b.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((b[i] & 0xFF) != (prefix[i] & 0xFF)) {
                return false;
            }
        }
        return true;
    }
}
