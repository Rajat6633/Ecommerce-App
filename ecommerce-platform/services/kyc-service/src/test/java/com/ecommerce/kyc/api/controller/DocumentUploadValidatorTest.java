package com.ecommerce.kyc.api.controller;

import com.ecommerce.kyc.api.exception.InvalidDocumentUploadException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentUploadValidatorTest {

    private static byte[] png() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
    }

    private static byte[] jpeg() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00};
    }

    private static byte[] webp() {
        return new byte[]{'R', 'I', 'F', 'F', 0x00, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P', 0x00};
    }

    @Test
    void acceptsValidPng() {
        assertThat(DocumentUploadValidator.validate(png(), "image/png")).isEqualTo("image/png");
    }

    @Test
    void acceptsValidJpegAndWebp() {
        assertThatCode(() -> DocumentUploadValidator.validate(jpeg(), "image/jpeg")).doesNotThrowAnyException();
        assertThatCode(() -> DocumentUploadValidator.validate(webp(), "image/webp")).doesNotThrowAnyException();
    }

    @Test
    void normalisesContentTypeCase() {
        assertThat(DocumentUploadValidator.validate(png(), "IMAGE/PNG")).isEqualTo("image/png");
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> DocumentUploadValidator.validate(new byte[0], "image/png"))
                .isInstanceOf(InvalidDocumentUploadException.class)
                .hasMessageContaining("empty");
        assertThatThrownBy(() -> DocumentUploadValidator.validate(null, "image/png"))
                .isInstanceOf(InvalidDocumentUploadException.class);
    }

    @Test
    void rejectsOversizedFile() {
        byte[] tooBig = new byte[(int) DocumentUploadValidator.MAX_BYTES + 1];
        tooBig[0] = (byte) 0x89; tooBig[1] = 0x50; tooBig[2] = 0x4E; tooBig[3] = 0x47;
        assertThatThrownBy(() -> DocumentUploadValidator.validate(tooBig, "image/png"))
                .isInstanceOf(InvalidDocumentUploadException.class)
                .hasMessageContaining("limit");
    }

    @Test
    void rejectsDisallowedContentType() {
        assertThatThrownBy(() -> DocumentUploadValidator.validate(new byte[]{'%', 'P', 'D', 'F'}, "application/pdf"))
                .isInstanceOf(InvalidDocumentUploadException.class)
                .hasMessageContaining("Unsupported");
        assertThatThrownBy(() -> DocumentUploadValidator.validate(png(), null))
                .isInstanceOf(InvalidDocumentUploadException.class);
    }

    @Test
    void rejectsMagicByteMismatch() {
        // declared PNG but bytes are JPEG -> forged/renamed file.
        assertThatThrownBy(() -> DocumentUploadValidator.validate(jpeg(), "image/png"))
                .isInstanceOf(InvalidDocumentUploadException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void rejectsJunkContentTypeWithoutThrowingFromMimeParser() {
        // attacker-controlled junk must produce a clean 400 path, not an unguarded parse error.
        assertThatThrownBy(() -> DocumentUploadValidator.validate(png(), "not/a/mime/../type"))
                .isInstanceOf(InvalidDocumentUploadException.class);
    }
}
