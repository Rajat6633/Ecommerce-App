package com.ecommerce.kyc.application.service;

import com.ecommerce.kyc.application.port.in.ExtractDocumentUseCase;
import com.ecommerce.kyc.application.port.in.KycQueryUseCase;
import com.ecommerce.kyc.application.port.in.ResolveCaseUseCase;
import com.ecommerce.kyc.application.port.in.ScreenCustomerUseCase;
import com.ecommerce.kyc.application.port.out.DocumentExtractionPort;
import com.ecommerce.kyc.application.port.out.DocumentExtractionPort.ExtractedDocument;
import com.ecommerce.kyc.application.port.out.IdentityVendorPort;
import com.ecommerce.kyc.application.port.out.IdentityVendorPort.VendorCheck;
import com.ecommerce.kyc.application.port.out.KycCaseRepositoryPort;
import com.ecommerce.kyc.application.port.out.KycEventPublisherPort;
import com.ecommerce.kyc.application.port.out.ProcessedEventPort;
import com.ecommerce.kyc.application.port.out.RiskNarrativePort;
import com.ecommerce.kyc.application.port.out.ScreeningPort;
import com.ecommerce.kyc.domain.exception.InvalidCaseStateException;
import com.ecommerce.kyc.domain.exception.KycCaseNotFoundException;
import com.ecommerce.kyc.domain.model.KycCase;
import com.ecommerce.kyc.domain.model.KycStatus;
import com.ecommerce.kyc.domain.model.RiskScore;
import com.ecommerce.kyc.domain.model.WatchlistHit;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the KYC saga: user.registered → open case → screen → decide.
 *
 * <p><strong>Fail closed.</strong> The screening / narrative ports already fold
 * AI outages into a safe default (a synthetic hit / fail-closed narrative). A
 * clean screen with no hits is the <em>only</em> path that auto-approves and
 * publishes {@code kyc.approved}; everything else parks the case in
 * MANUAL_REVIEW and publishes nothing until an officer resolves it.
 *
 * <p>Idempotent per {@code eventId} (processed_events ledger) and per
 * {@code userId} (unique case), inside the state-changing transaction.
 */
@Service
public class KycService implements ScreenCustomerUseCase, ResolveCaseUseCase,
        KycQueryUseCase, ExtractDocumentUseCase {

    private static final Logger log = LoggerFactory.getLogger(KycService.class);

    private final KycCaseRepositoryPort caseRepository;
    private final ProcessedEventPort processedEvents;
    private final ScreeningPort screening;
    private final DocumentExtractionPort documentExtraction;
    private final RiskNarrativePort riskNarrative;
    private final KycEventPublisherPort eventPublisher;
    private final IdentityVendorPort identityVendor;
    private final Clock clock;
    private final double riskThreshold;

    private final Counter casesApproved;
    private final Counter casesManualReview;
    private final Counter watchlistHits;

    public KycService(KycCaseRepositoryPort caseRepository,
                      ProcessedEventPort processedEvents,
                      ScreeningPort screening,
                      DocumentExtractionPort documentExtraction,
                      RiskNarrativePort riskNarrative,
                      KycEventPublisherPort eventPublisher,
                      IdentityVendorPort identityVendor,
                      Clock clock,
                      KycProperties properties,
                      MeterRegistry meterRegistry) {
        this.caseRepository = caseRepository;
        this.processedEvents = processedEvents;
        this.screening = screening;
        this.documentExtraction = documentExtraction;
        this.riskNarrative = riskNarrative;
        this.eventPublisher = eventPublisher;
        this.identityVendor = identityVendor;
        this.clock = clock;
        this.riskThreshold = properties.riskThreshold();
        this.casesApproved = Counter.builder("kyc_cases_total").tag("status", "APPROVED")
                .description("KYC cases that ended APPROVED").register(meterRegistry);
        this.casesManualReview = Counter.builder("kyc_cases_total").tag("status", "MANUAL_REVIEW")
                .description("KYC cases routed to manual review").register(meterRegistry);
        this.watchlistHits = Counter.builder("kyc_watchlist_hits_total")
                .description("Total watchlist hits raised across cases").register(meterRegistry);
    }

    // -------------------------------------------------------------------------
    //  user.registered → screen
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public ScreeningOutcome onUserRegistered(UUID eventId, UUID userId, String email) {
        if (!processedEvents.firstSeen(eventId) || caseRepository.existsByUserId(userId)) {
            log.info("user.registered for user {} (event {}) already processed — skipping", userId, eventId);
            return ScreeningOutcome.alreadyProcessed();
        }

        KycCase opened = caseRepository.save(KycCase.open(UUID.randomUUID(), userId, clock.instant()));
        KycCase inProgress = caseRepository.save(opened.inProgress(clock.instant()));

        // Screening derives a name from the email local-part — the platform has no
        // profile lookup at this stage; document upload refines it later.
        String fullName = nameFromEmail(email, userId);
        List<WatchlistHit> hits = screening.screen(fullName);
        String narrative = riskNarrative.summarise(fullName, hits);

        KycCase resolved;
        if (hits.isEmpty()) {
            resolved = caseRepository.save(inProgress.approved(new RiskScore(0.0, narrative), clock.instant()));
            casesApproved.increment();
            eventPublisher.publishApproved(userId);
            log.info("KYC clean screen — user {} APPROVED, kyc.approved published", userId);
        } else {
            watchlistHits.increment(hits.size());
            RiskScore score = new RiskScore(highestScore(hits), narrative);
            resolved = caseRepository.save(inProgress.manualReview(score, hits,
                    "Watchlist hit(s) require officer review", clock.instant()));
            casesManualReview.increment();
            log.warn("KYC screening raised {} hit(s) — user {} routed to MANUAL_REVIEW (no event)",
                    hits.size(), userId);
        }
        return ScreeningOutcome.screened(resolved);
    }

    // -------------------------------------------------------------------------
    //  officer resolve → publish
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public KycCase resolve(UUID userId, String officer, boolean approve, String reason) {
        KycCase existing = caseRepository.findByUserId(userId)
                .orElseThrow(() -> new KycCaseNotFoundException(userId));
        if (existing.status() != KycStatus.MANUAL_REVIEW) {
            throw new InvalidCaseStateException(existing.status());
        }

        KycCase resolved = caseRepository.save(existing.resolvedBy(officer, approve, reason, clock.instant()));
        if (approve) {
            casesApproved.increment();
            eventPublisher.publishApproved(userId);
            log.info("Officer {} APPROVED user {} — kyc.approved published", officer, userId);
        } else {
            eventPublisher.publishRejected(userId, reason);
            log.info("Officer {} REJECTED user {} — kyc.rejected published", officer, userId);
        }
        return resolved;
    }

    // -------------------------------------------------------------------------
    //  queries
    // -------------------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public KycCase getByUserId(UUID userId) {
        return caseRepository.findByUserId(userId)
                .orElseThrow(() -> new KycCaseNotFoundException(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycCase> getByStatus(KycStatus status, int page, int size) {
        return caseRepository.findByStatus(status, page, size);
    }

    // -------------------------------------------------------------------------
    //  document upload → extraction (observe-only; records extracted fields)
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public ExtractedDocument uploadDocument(UUID userId, byte[] imageBytes, String mediaType) {
        KycCase existing = caseRepository.findByUserId(userId)
                .orElseThrow(() -> new KycCaseNotFoundException(userId));

        ExtractedDocument extracted = documentExtraction.extract(imageBytes, mediaType);

        // Optional vendor liveness/face check (simulated unless kyc.vendor.enabled).
        VendorCheck vendorCheck = identityVendor.verify(userId);
        if (!extracted.confident() || !vendorCheck.passed()) {
            // Fail closed: if either signal is unreliable, the case must be reviewed.
            if (existing.status() != KycStatus.MANUAL_REVIEW && !existing.isTerminal()) {
                caseRepository.save(existing.manualReview(
                        RiskScore.failClosed("document extraction or vendor check inconclusive"),
                        existing.hits(),
                        "Document/vendor verification inconclusive", clock.instant()));
                casesManualReview.increment();
            }
            log.warn("Document/vendor verification inconclusive for user {} (confident={}, vendorPassed={})",
                    userId, extracted.confident(), vendorCheck.passed());
        } else {
            log.info("Document extracted for user {} (vendor ref {})", userId, vendorCheck.reference());
        }
        return extracted;
    }

    private static String nameFromEmail(String email, UUID userId) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return "user-" + userId;
        }
        return email.substring(0, email.indexOf('@')).replace('.', ' ').replace('_', ' ').trim();
    }

    private static double highestScore(List<WatchlistHit> hits) {
        return hits.stream().mapToDouble(WatchlistHit::score).max().orElse(1.0);
    }
}
