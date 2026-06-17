package com.ecommerce.order.application.service;

import com.ecommerce.order.application.port.in.RecordKycDecisionUseCase;
import com.ecommerce.order.application.port.out.CustomerKycStatusPort;
import com.ecommerce.order.application.port.out.ProcessedEventPort;
import com.ecommerce.order.domain.model.KycStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Maintains the local {@code customer_kyc_status} read-model from the kyc.* stream.
 * Each handler is @Transactional so the idempotency record (firstSeen) commits
 * atomically with the upsert; duplicate redeliveries (same eventId) are skipped.
 */
@Service
public class CustomerKycService implements RecordKycDecisionUseCase {

    private static final Logger log = LoggerFactory.getLogger(CustomerKycService.class);

    private final CustomerKycStatusPort kycStatus;
    private final ProcessedEventPort processedEvents;
    private final Clock clock;

    public CustomerKycService(CustomerKycStatusPort kycStatus,
                              ProcessedEventPort processedEvents,
                              Clock clock) {
        this.kycStatus = kycStatus;
        this.processedEvents = processedEvents;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void onKycApproved(UUID eventId, UUID userId) {
        if (!processedEvents.firstSeen(eventId)) return;
        kycStatus.upsert(userId, KycStatus.APPROVED, clock.instant());
        log.info("KYC read-model updated userId={} status=APPROVED", userId);
    }

    @Override
    @Transactional
    public void onKycRejected(UUID eventId, UUID userId, String reason) {
        if (!processedEvents.firstSeen(eventId)) return;
        kycStatus.upsert(userId, KycStatus.REJECTED, clock.instant());
        log.info("KYC read-model updated userId={} status=REJECTED reason={}", userId, reason);
    }
}
