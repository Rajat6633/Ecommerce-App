package com.ecommerce.kyc.api.dto;

import com.ecommerce.kyc.application.port.out.DocumentExtractionPort.ExtractedDocument;

public record DocumentExtractionResponse(
        String fullName,
        String documentNumber,
        String dateOfBirth,
        String expiry,
        String nationality,
        boolean confident
) {
    public static DocumentExtractionResponse from(ExtractedDocument d) {
        return new DocumentExtractionResponse(d.fullName(), d.documentNumber(), d.dateOfBirth(),
                d.expiry(), d.nationality(), d.confident());
    }
}
