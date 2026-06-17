package com.ecommerce.order.application.service;

import com.ecommerce.order.application.port.out.OrderRepositoryPort;
import com.ecommerce.order.application.port.out.ProcessedEventPort;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderItem;
import com.ecommerce.order.domain.model.OrderStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSagaServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-10T00:00:00Z");

    @Mock OrderRepositoryPort orderRepository;
    @Mock ProcessedEventPort processedEvents;

    private SimpleMeterRegistry meterRegistry;
    private OrderSagaService service;

    private final UUID eventId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new OrderSagaService(orderRepository, processedEvents,
                Clock.fixed(NOW, ZoneOffset.UTC), meterRegistry);
    }

    private Order orderIn(OrderStatus status) {
        return new Order(orderId, UUID.randomUUID(), status, new BigDecimal("10.00"), "USD",
                List.of(new OrderItem(UUID.randomUUID(), 1, new BigDecimal("10.00"))), 0L, NOW, NOW);
    }

    @Test
    void onPaymentCompleted_confirmsOrderAndCounts() {
        when(processedEvents.firstSeen(eventId)).thenReturn(true);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(orderIn(OrderStatus.INVENTORY_RESERVED)));
        when(orderRepository.updateStatus(any(), any())).thenAnswer(i -> i.getArgument(0));

        Optional<Order> result = service.onPaymentCompleted(eventId, orderId);

        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(meterRegistry.get("orders_completed_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void onPaymentCompleted_duplicateEvent_noop() {
        when(processedEvents.firstSeen(eventId)).thenReturn(false);

        assertThat(service.onPaymentCompleted(eventId, orderId)).isEmpty();
        verify(orderRepository, never()).findById(any());
    }

    @Test
    void onPaymentCompleted_terminalOrder_skips() {
        when(processedEvents.firstSeen(eventId)).thenReturn(true);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(orderIn(OrderStatus.REJECTED)));

        assertThat(service.onPaymentCompleted(eventId, orderId)).isEmpty();
        verify(orderRepository, never()).updateStatus(any(), any());
    }

    @Test
    void onPaymentFailed_marksPaymentFailedAndCounts() {
        when(processedEvents.firstSeen(eventId)).thenReturn(true);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(orderIn(OrderStatus.INVENTORY_RESERVED)));
        when(orderRepository.updateStatus(any(), any())).thenAnswer(i -> i.getArgument(0));

        service.onPaymentFailed(eventId, orderId, "declined");

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).updateStatus(captor.capture(), eq("payment failed: declined"));
        assertThat(captor.getValue().status()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(meterRegistry.get("orders_failed_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void onInventoryReservationFailed_rejectsOrder() {
        when(processedEvents.firstSeen(eventId)).thenReturn(true);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(orderIn(OrderStatus.PENDING)));
        when(orderRepository.updateStatus(any(), any())).thenAnswer(i -> i.getArgument(0));

        service.onInventoryReservationFailed(eventId, orderId, "out of stock");

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).updateStatus(captor.capture(), any());
        assertThat(captor.getValue().status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(meterRegistry.get("orders_failed_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void onInventoryReserved_advancesStatus() {
        when(processedEvents.firstSeen(eventId)).thenReturn(true);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(orderIn(OrderStatus.PENDING)));
        when(orderRepository.updateStatus(any(), any())).thenAnswer(i -> i.getArgument(0));

        service.onInventoryReserved(eventId, orderId);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).updateStatus(captor.capture(), any());
        assertThat(captor.getValue().status()).isEqualTo(OrderStatus.INVENTORY_RESERVED);
    }

    @Test
    void onInventoryReserved_duplicateEvent_noop() {
        when(processedEvents.firstSeen(eventId)).thenReturn(false);

        service.onInventoryReserved(eventId, orderId);

        verify(orderRepository, never()).findById(any());
    }
}
