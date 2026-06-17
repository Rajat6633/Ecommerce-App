package com.ecommerce.payment.application.service;

import com.ecommerce.payment.application.port.in.ProcessPaymentUseCase.PaymentOutcome;
import com.ecommerce.payment.application.port.in.ProcessPaymentUseCase.Status;
import com.ecommerce.payment.application.port.out.PaymentGatewayPort;
import com.ecommerce.payment.application.port.out.PaymentGatewayPort.ChargeResult;
import com.ecommerce.payment.application.port.out.PaymentRepositoryPort;
import com.ecommerce.payment.application.port.out.ProcessedEventPort;
import com.ecommerce.payment.domain.exception.PaymentNotFoundException;
import com.ecommerce.payment.domain.model.Payment;
import com.ecommerce.payment.domain.model.PaymentStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-10T00:00:00Z");

    @Mock PaymentRepositoryPort paymentRepository;
    @Mock PaymentGatewayPort gateway;
    @Mock ProcessedEventPort processedEvents;

    private SimpleMeterRegistry meterRegistry;
    private PaymentService service;

    private final UUID eventId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final BigDecimal amount = new BigDecimal("39.98");

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new PaymentService(paymentRepository, gateway, processedEvents,
                Clock.fixed(NOW, ZoneOffset.UTC), meterRegistry);
    }

    @Test
    void process_approved_completesAndCounts() {
        when(processedEvents.firstSeen(eventId)).thenReturn(true);
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(gateway.charge(orderId, amount, "USD")).thenReturn(ChargeResult.approve());
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PaymentOutcome outcome = service.process(eventId, orderId, amount, "USD");

        assertThat(outcome.status()).isEqualTo(Status.COMPLETED);
        assertThat(outcome.payment().status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(meterRegistry.get("payments_processed_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void process_declined_failsAndCounts() {
        when(processedEvents.firstSeen(eventId)).thenReturn(true);
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(gateway.charge(orderId, amount, "USD")).thenReturn(ChargeResult.decline("PAYMENT_DECLINED"));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PaymentOutcome outcome = service.process(eventId, orderId, amount, "USD");

        assertThat(outcome.status()).isEqualTo(Status.FAILED);
        assertThat(outcome.failureReason()).isEqualTo("PAYMENT_DECLINED");
        assertThat(outcome.payment().status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(meterRegistry.get("payment_failures_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void process_duplicateEvent_alreadyProcessed() {
        when(processedEvents.firstSeen(eventId)).thenReturn(false);

        PaymentOutcome outcome = service.process(eventId, orderId, amount, "USD");

        assertThat(outcome.status()).isEqualTo(Status.ALREADY_PROCESSED);
        verify(gateway, never()).charge(any(), any(), any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void process_existingPaymentForOrder_alreadyProcessed() {
        when(processedEvents.firstSeen(eventId)).thenReturn(true);
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(true);

        PaymentOutcome outcome = service.process(eventId, orderId, amount, "USD");

        assertThat(outcome.status()).isEqualTo(Status.ALREADY_PROCESSED);
        verify(gateway, never()).charge(any(), any(), any());
    }

    @Test
    void getByOrderId_missing_throwsNotFound() {
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByOrderId(orderId))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void getByOrderId_present_returnsPayment() {
        Payment p = Payment.initiate(UUID.randomUUID(), orderId, amount, "USD", NOW).completed(NOW);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(p));

        assertThat(service.getByOrderId(orderId).status()).isEqualTo(PaymentStatus.COMPLETED);
    }
}
