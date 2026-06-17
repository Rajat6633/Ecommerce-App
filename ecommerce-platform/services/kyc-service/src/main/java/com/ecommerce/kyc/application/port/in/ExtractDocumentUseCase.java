package com.ecommerce.kyc.application.port.in;

import com.ecommerce.kyc.application.port.out.DocumentExtractionPort.ExtractedDocument;

import java.util.UUID;

/** Owner uploads an ID document → triggers multimodal extraction. */
public interface ExtractDocumentUseCase {

    ExtractedDocument uploadDocument(UUID userId, byte[] imageBytes, String mediaType);
}
