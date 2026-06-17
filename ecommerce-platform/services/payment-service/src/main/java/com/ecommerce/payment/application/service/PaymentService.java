package com.ecommerce.payment.application.service;

import com.ecommerce.payment.application.port.in.PaymentQueryUseCase;
import com.ecommerce.payment.application.port.in.ProcessPaymentUseCase;
import com.ecommerce.payment.application.port.out.PaymentGatewayPort;
import com.ecommerce.payment.application.port.out.PaymentGatewayPort.ChargeResult;
import com.ecommerce.payment.application.port.out.PaymentRepositoryPort;
import com.ecommerce.payment.application.port.out.ProcessedEventPort;
import com.ecommerce.payment.domain.exception.PaymentNotFoundException;
import com.ecommerce.payment.domain.model.Payment;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

/**
 * Processes payments triggered by inventory.reserved. Idempotent per event and
 * per order; the outcome is returned so the consumer publishes the matching
 * payment event after commit.
 */
@Service
public class PaymentService implements ProcessPaymentUseCase, PaymentQueryUseCase {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepositoryPort paymentRepository;
    private final PaymentGatewayPort gateway;
    private final ProcessedEventPort processedEvents;
    private final Clock clock;
    private final Counter paymentsProcessed;
    private final Counter paymentFailures;

    public PaymentService(PaymentRepositoryPort paymentRepository,
                          PaymentGatewayPort gateway,
                          ProcessedEventPort processedEvents,
                          Clock clock,
                          MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.gateway = gateway;
        this.processedEvents = processedEvents;
        this.clock = clock;
        this.paymentsProcessed = Counter.builder("payments_processed_total")
                .description("Total payments completed successfully").register(meterRegistry);
        this.paymentFailures = Counter.builder("payment_failures_total")
                .description("Total payments that failed").register(meterRegistry);
    }

    @Override
    @Transactional
    public PaymentOutcome process(UUID eventId, UUID orderId, BigDecimal amount, String currency) {
        if (!processedEvents.firstSeen(eventId) || paymentRepository.existsByOrderId(orderId)) {
            log.info("Payment for order {} (event {}) already processed — skipping", orderId, eventId);
            return PaymentOutcome.alreadyProcessed();
        }

        Payment payment = Payment.initiate(UUID.randomUUID(), orderId, amount, currency, clock.instant());
        ChargeResult result = gateway.charge(orderId, amount, currency);

        if (result.approved()) {
            Payment completed = paymentRepository.save(payment.completed(clock.instant()));
            paymentsProcessed.increment();
            log.info("Payment completed orderId={} paymentId={}", orderId, completed.id());
            return PaymentOutcome.completed(completed);
        }

        Payment failed = paymentRepository.save(payment.failed(result.declineReason(), clock.instant()));
        paymentFailures.increment();
        log.warn("Payment failed orderId={} reason={}", orderId, result.declineReason());
        return PaymentOutcome.failed(failed, result.declineReason());
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId.toString()));
    }
}
