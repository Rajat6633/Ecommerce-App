package com.ecommerce.kyc.api.controller;

import com.ecommerce.kyc.api.dto.DocumentExtractionResponse;
import com.ecommerce.kyc.api.dto.KycCaseResponse;
import com.ecommerce.kyc.api.dto.ResolveCaseRequest;
import com.ecommerce.kyc.application.port.in.ExtractDocumentUseCase;
import com.ecommerce.kyc.application.port.in.KycQueryUseCase;
import com.ecommerce.kyc.application.port.in.ResolveCaseUseCase;
import com.ecommerce.kyc.application.port.out.UploadRateLimiterPort;
import com.ecommerce.kyc.api.exception.InvalidSubjectException;
import com.ecommerce.kyc.domain.model.KycStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * KYC API (docs/19 §6). Self endpoints derive the user from the JWT subject;
 * officer endpoints require ROLE_COMPLIANCE.
 */
@RestController
@RequestMapping("/api/kyc")
@Tag(name = "KYC", description = "Identity verification + sanctions screening")
@SecurityRequirement(name = "bearerAuth")
public class KycController {

    private final KycQueryUseCase query;
    private final ResolveCaseUseCase resolveCase;
    private final ExtractDocumentUseCase extractDocument;
    private final UploadRateLimiterPort uploadRateLimiter;

    public KycController(KycQueryUseCase query, ResolveCaseUseCase resolveCase,
                         ExtractDocumentUseCase extractDocument,
                         UploadRateLimiterPort uploadRateLimiter) {
        this.query = query;
        this.resolveCase = resolveCase;
        this.extractDocument = extractDocument;
        this.uploadRateLimiter = uploadRateLimiter;
    }

    @GetMapping("/me")
    @Operation(summary = "Caller's own KYC status")
    public KycCaseResponse myStatus(Authentication auth) {
        return KycCaseResponse.from(query.getByUserId(userId(auth)));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('COMPLIANCE')")
    @Operation(summary = "Case detail incl. hits + narrative (compliance officer)")
    public KycCaseResponse caseDetail(@PathVariable UUID userId) {
        return KycCaseResponse.from(query.getByUserId(userId));
    }

    @GetMapping("/cases")
    @PreAuthorize("hasRole('COMPLIANCE')")
    @Operation(summary = "Review queue, filtered by status (paginated)")
    public List<KycCaseResponse> queue(@RequestParam(defaultValue = "MANUAL_REVIEW") KycStatus status,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return query.getByStatus(status, page, size).stream().map(KycCaseResponse::from).toList();
    }

    @PostMapping("/{userId}/resolve")
    @PreAuthorize("hasRole('COMPLIANCE')")
    @Operation(summary = "Officer decision → emits kyc.approved / kyc.rejected")
    public KycCaseResponse resolve(@PathVariable UUID userId,
                                   @Valid @RequestBody ResolveCaseRequest request,
                                   Authentication auth) {
        return KycCaseResponse.from(
                resolveCase.resolve(userId, auth.getName(), request.approve(), request.reason()));
    }

    @PostMapping("/{userId}/documents")
    @PreAuthorize("#userId.toString() == authentication.name")
    @Operation(summary = "Upload an ID document (owner) → triggers extraction")
    public DocumentExtractionResponse uploadDocument(@PathVariable UUID userId,
                                                     @RequestParam("file") MultipartFile file) throws IOException {
        // Per-user rate limit (roadmap C3): one user can't exhaust the shared AI
        // budget. userId is path-bound and already owner-validated by @PreAuthorize.
        uploadRateLimiter.acquire(userId);
        byte[] bytes = file.getBytes();
        // Reject empty/oversized/unsupported/forged uploads with 400 before extraction.
        String contentType = DocumentUploadValidator.validate(bytes, file.getContentType());
        return DocumentExtractionResponse.from(
                extractDocument.uploadDocument(userId, bytes, contentType));
    }

    private static UUID userId(Authentication auth) {
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            // A token whose subject is not a UUID is a bad request, not a 500.
            throw new InvalidSubjectException(auth.getName());
        }
    }
}
