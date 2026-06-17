package com.ecommerce.order.application.service;

import com.ecommerce.order.application.port.out.CustomerKycStatusPort;
import com.ecommerce.order.application.port.out.ProcessedEventPort;
import com.ecommerce.order.domain.model.KycStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerKycServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-10T00:00:00Z");

    @Mock CustomerKycStatusPort kycStatus;
    @Mock ProcessedEventPort processedEvents;

    private CustomerKycService service;

    private final UUID eventId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CustomerKycService(kycStatus, processedEvents, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void onKycApproved_recordsApproved() {
        when(processedEvents.firstSeen(eventId)).thenReturn(true);

        service.onKycApproved(eventId, userId);

        verify(kycStatus).upsert(eq(userId), eq(KycStatus.APPROVED), eq(NOW));
    }

    @Test
    void onKycRejected_recordsRejected() {
        when(processedEvents.firstSeen(eventId)).thenReturn(true);

        service.onKycRejected(eventId, userId, "watchlist hit");

        verify(kycStatus).upsert(eq(userId), eq(KycStatus.REJECTED), eq(NOW));
    }

    @Test
    void onKycApproved_duplicateEvent_skipped() {
        when(processedEvents.firstSeen(eventId)).thenReturn(false);

        service.onKycApproved(eventId, userId);

        verify(kycStatus, never()).upsert(any(), any(), any());
    }

    @Test
    void onKycRejected_duplicateEvent_skipped() {
        when(processedEvents.firstSeen(eventId)).thenReturn(false);

        service.onKycRejected(eventId, userId, "watchlist hit");

        verify(kycStatus, never()).upsert(any(), any(), any());
    }
}
