package com.ecommerce.kyc.api.controller;

import com.ecommerce.kyc.api.exception.GlobalExceptionHandler;
import com.ecommerce.kyc.application.port.in.ExtractDocumentUseCase;
import com.ecommerce.kyc.application.port.in.KycQueryUseCase;
import com.ecommerce.kyc.application.port.in.ResolveCaseUseCase;
import com.ecommerce.kyc.application.port.out.DocumentExtractionPort.ExtractedDocument;
import com.ecommerce.kyc.application.port.out.UploadRateLimiterPort;
import com.ecommerce.kyc.application.port.out.UploadRateLimiterPort.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the per-user upload rate limit surfaces as HTTP 429 through the
 * controller + {@link GlobalExceptionHandler}. Offline: standalone MockMvc, no
 * Spring context and no security filter (owner-gating via @PreAuthorize is tested
 * elsewhere; here we drive the path directly with the userId already validated).
 */
class KycControllerUploadRateLimitTest {

    private final KycQueryUseCase query = Mockito.mock(KycQueryUseCase.class);
    private final ResolveCaseUseCase resolveCase = Mockito.mock(ResolveCaseUseCase.class);
    private final ExtractDocumentUseCase extractDocument = Mockito.mock(ExtractDocumentUseCase.class);
    private final UploadRateLimiterPort rateLimiter = Mockito.mock(UploadRateLimiterPort.class);

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        KycController controller = new KycController(query, resolveCase, extractDocument, rateLimiter);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static MockMultipartFile pngFile() {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
        return new MockMultipartFile("file", "id.png", MediaType.IMAGE_PNG_VALUE, png);
    }

    @Test
    void underLimit_uploadProceeds() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(rateLimiter).acquire(any());
        when(extractDocument.uploadDocument(any(), any(), any()))
                .thenReturn(new ExtractedDocument("Jane Doe", "X1", "2000-01-01", "2030-01-01", "US", true));

        mvc.perform(multipart("/api/kyc/" + userId + "/documents").file(pngFile()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Jane Doe"));
    }

    @Test
    void overLimit_returns429WithErrorResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        doThrow(new RateLimitExceededException("Upload rate limit exceeded: max 5 per minute"))
                .when(rateLimiter).acquire(any());

        mvc.perform(multipart("/api/kyc/" + userId + "/documents").file(pngFile()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("Upload rate limit exceeded: max 5 per minute"));

        // Extraction must NOT run once the user is over budget.
        Mockito.verify(extractDocument, Mockito.never()).uploadDocument(any(), any(), any());
    }
}
