package com.ecommerce.kyc.application.port.out;

/**
 * Multimodal ID-document data extraction (Claude vision). Returns the extracted
 * structured fields; fails closed (empty result flagged for review) on outage.
 */
public interface DocumentExtractionPort {

    ExtractedDocument extract(byte[] imageBytes, String mediaType);

    /** Structured fields read off an ID document. {@code confident} is false on a fail-closed fallback. */
    record ExtractedDocument(
            String fullName,
            String documentNumber,
            String dateOfBirth,
            String expiry,
            String nationality,
            boolean confident
    ) {
        public static ExtractedDocument lowConfidence() {
            return new ExtractedDocument(null, null, null, null, null, false);
        }
    }
}
