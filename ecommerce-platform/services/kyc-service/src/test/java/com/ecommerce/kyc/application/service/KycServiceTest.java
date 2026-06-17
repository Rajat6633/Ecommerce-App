package com.ecommerce.kyc.application.service;

import com.ecommerce.kyc.application.port.in.ScreenCustomerUseCase.ScreeningOutcome;
import com.ecommerce.kyc.application.port.in.ScreenCustomerUseCase.Status;
import com.ecommerce.kyc.application.port.out.DocumentExtractionPort;
import com.ecommerce.kyc.application.port.out.IdentityVendorPort;
import com.ecommerce.kyc.application.port.out.KycCaseRepositoryPort;
import com.ecommerce.kyc.application.port.out.KycEventPublisherPort;
import com.ecommerce.kyc.application.port.out.ProcessedEventPort;
import com.ecommerce.kyc.application.port.out.RiskNarrativePort;
import com.ecommerce.kyc.application.port.out.ScreeningPort;
import com.ecommerce.kyc.domain.exception.InvalidCaseStateException;
import com.ecommerce.kyc.domain.model.KycCase;
import com.ecommerce.kyc.domain.model.KycStatus;
import com.ecommerce.kyc.domain.model.WatchlistHit;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KycServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");

    @Mock KycCaseRepositoryPort caseRepository;
    @Mock ProcessedEventPort processedEvents;
    @Mock ScreeningPort screening;
    @Mock DocumentExtractionPort documentExtraction;
    @Mock RiskNarrativePort riskNarrative;
    @Mock KycEventPublisherPort eventPublisher;
    @Mock IdentityVendorPort identityVendor;

    private KycService service;

    private final UUID eventId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new KycService(caseRepository, processedEvents, screening, documentExtraction,
                riskNarrative, eventPublisher, identityVendor,
                Clock.fixed(NOW, ZoneOffset.UTC), new KycProperties(0.85, false), new SimpleMeterRegistry());
    }

    @Test
    void userRegistered_opensCaseAndScreens_cleanApprovesAndPublishes() {
        when(processedEvents.firstSeen(eventId)).thenReturn(true);
        when(caseRepository.existsByUserId(userId)).thenReturn(false);
        when(caseRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(screening.screen(anyString())).thenReturn(List.of());
        when(riskNarrative.summarise(anyString(), eq(List.of()))).thenReturn("clean");

        ScreeningOutcome outcome = service.onUserRegistered(eventId, userId, "jane.doe@example.com");

        assertThat(outcome.status()).isEqualTo(Status.SCREENED);
        assertThat(outcome.kycCase().status()).isEqualTo(KycStatus.APPROVED);
        verify(eventPublisher).publishApproved(userId);
    }

    @Test
    void userRegistered_passesThroughPendingThenInProgress() {
        when(processedEvents.firstSeen(eventId)).thenReturn(true);
        when(caseRepository.existsByUserId(userId)).thenReturn(false);
        when(caseRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(screening.screen(anyString())).thenReturn(List.of());
        when(riskNarrative.summarise(anyString(), any())).thenReturn("clean");

        service.onUserRegistered(eventId, userId, "jane@example.com");

        // first save is the freshly-opened PENDING case
        org.mockito.ArgumentCaptor<KycCase> captor = org.mockito.ArgumentCaptor.forClass(KycCase.class);
        verify(caseRepository, org.mockito.Mockito.atLeast(3)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).status()).isEqualTo(KycStatus.PENDING);
        assertThat(captor.getAllValues().get(1).status()).isEqualTo(KycStatus.IN_PROGRESS);
    }

    @Test
    void userRegistered_watchlistHit_routesToManualReview_noEvent() {
        when(processedEvents.firstSeen(eventId)).thenReturn(true);
        when(caseRepository.existsByUserId(userId)).thenReturn(false);
        when(caseRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(screening.screen(anyString()))
                .thenReturn(List.of(WatchlistHit.of("OFAC", "Viktor Petrov", 0.93, "raw")));
        when(riskNarrative.summarise(anyString(), any())).thenReturn("hit narrative");

        ScreeningOutcome outcome = service.onUserRegistered(eventId, userId, "viktor.petrov@example.com");

        assertThat(outcome.kycCase().status()).isEqualTo(KycStatus.MANUAL_REVIEW);
        verify(eventPublisher, never()).publishApproved(any());
        verify(eventPublisher, never()).publishRejected(any(), anyString());
    }

    @Test
    void userRegistered_duplicateEvent_skipped() {
        when(processedEvents.firstSeen(eventId)).thenReturn(false);

        ScreeningOutcome outcome = service.onUserRegistered(eventId, userId, "x@example.com");

        assertThat(outcome.status()).isEqualTo(Status.ALREADY_PROCESSED);
        verify(screening, never()).screen(anyString());
        verify(caseRepository, never()).save(any());
    }

    @Test
    void officerResolve_approve_publishesApproved() {
        KycCase inReview = KycCase.open(UUID.randomUUID(), userId, NOW)
                .manualReview(null, List.of(), "review", NOW);
        when(caseRepository.findByUserId(userId)).thenReturn(Optional.of(inReview));
        when(caseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        KycCase resolved = service.resolve(userId, "officer-1", true, "cleared after review");

        assertThat(resolved.status()).isEqualTo(KycStatus.APPROVED);
        assertThat(resolved.resolvedBy()).isEqualTo("officer-1");
        verify(eventPublisher).publishApproved(userId);
    }

    @Test
    void officerResolve_reject_publishesRejected() {
        KycCase inReview = KycCase.open(UUID.randomUUID(), userId, NOW)
                .manualReview(null, List.of(), "review", NOW);
        when(caseRepository.findByUserId(userId)).thenReturn(Optional.of(inReview));
        when(caseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        KycCase resolved = service.resolve(userId, "officer-2", false, "sanctioned");

        assertThat(resolved.status()).isEqualTo(KycStatus.REJECTED);
        verify(eventPublisher).publishRejected(userId, "sanctioned");
    }

    @Test
    void officerResolve_nonReviewCase_rejected() {
        KycCase approved = KycCase.open(UUID.randomUUID(), userId, NOW)
                .approved(null, NOW);
        when(caseRepository.findByUserId(userId)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> service.resolve(userId, "officer", true, "x"))
                .isInstanceOf(InvalidCaseStateException.class);
        verify(eventPublisher, never()).publishApproved(any());
    }
}
