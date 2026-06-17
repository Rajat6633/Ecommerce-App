package com.ecommerce.order.infrastructure.messaging;

import com.ecommerce.common.events.EventEnvelope;
import com.ecommerce.common.events.Topics;
import com.ecommerce.common.events.payload.KycApprovedEvent;
import com.ecommerce.common.events.payload.KycRejectedEvent;
import com.ecommerce.order.application.port.in.RecordKycDecisionUseCase;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Projects kyc-service's {@code kyc.approved} / {@code kyc.rejected} events into the
 * local KYC read-model (choreographed saga — no synchronous call to kyc-service).
 * Failures retry then route to {@code kyc.approved.DLT} / {@code kyc.rejected.DLT}
 * via the shared {@link KafkaConfig} error handler.
 */
@Component
public class KycEventsConsumer {

    private final RecordKycDecisionUseCase recorder;

    public KycEventsConsumer(RecordKycDecisionUseCase recorder) {
        this.recorder = recorder;
    }

    @KafkaListener(topics = Topics.KYC_APPROVED)
    public void onApproved(EventEnvelope<KycApprovedEvent> envelope) {
        recorder.onKycApproved(envelope.eventId(), envelope.payload().userId());
    }

    @KafkaListener(topics = Topics.KYC_REJECTED)
    public void onRejected(EventEnvelope<KycRejectedEvent> envelope) {
        KycRejectedEvent e = envelope.payload();
        recorder.onKycRejected(envelope.eventId(), e.userId(), e.reason());
    }
}
