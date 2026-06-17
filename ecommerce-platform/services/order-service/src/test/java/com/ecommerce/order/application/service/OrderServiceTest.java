package com.ecommerce.order.application.service;

import com.ecommerce.order.application.port.out.CartClientPort;
import com.ecommerce.order.application.port.out.CartClientPort.CartSnapshot;
import com.ecommerce.order.application.port.out.CartClientPort.Line;
import com.ecommerce.order.application.port.out.CustomerKycStatusPort;
import com.ecommerce.order.application.port.out.OrderEventPublisherPort;
import com.ecommerce.order.application.port.out.OrderRepositoryPort;
import com.ecommerce.order.domain.exception.CartServiceUnavailableException;
import com.ecommerce.order.domain.exception.EmptyCartException;
import com.ecommerce.order.domain.exception.KycNotApprovedException;
import com.ecommerce.order.domain.exception.OrderNotFoundException;
import com.ecommerce.order.domain.model.CustomerKycStatus;
import com.ecommerce.order.domain.model.KycStatus;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderStatus;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-10T00:00:00Z");

    @Mock OrderRepositoryPort orderRepository;
    @Mock CartClientPort cartClient;
    @Mock OrderEventPublisherPort publisher;
    @Mock CustomerKycStatusPort kycStatus;

    private SimpleMeterRegistry meterRegistry;
    private OrderService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = serviceWithGating(true);
    }

    private OrderService serviceWithGating(boolean gatingEnabled) {
        return new OrderService(orderRepository, cartClient, publisher, kycStatus,
                new OrderKycProperties(gatingEnabled),
                Clock.fixed(NOW, ZoneOffset.UTC), meterRegistry, "USD");
    }

    private void kycApproved() {
        when(kycStatus.findByUserId(userId)).thenReturn(
                Optional.of(new CustomerKycStatus(userId, KycStatus.APPROVED, NOW)));
    }

    @Test
    void placeOrder_emptyCart_throws() {
        kycApproved();
        when(cartClient.getCart(userId)).thenReturn(new CartSnapshot(List.of()));

        assertThatThrownBy(() -> service.placeOrder(userId)).isInstanceOf(EmptyCartException.class);
        verify(orderRepository, never()).createPending(any());
        verify(publisher, never()).publishOrderCreated(any());
    }

    @Test
    void placeOrder_happyPath_persistsPublishesAndCounts() {
        kycApproved();
        when(cartClient.getCart(userId)).thenReturn(
                new CartSnapshot(List.of(new Line(productId, 2, new BigDecimal("9.99")))));
        when(orderRepository.createPending(any())).thenAnswer(i -> i.getArgument(0));

        Order result = service.placeOrder(userId);

        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.items()).hasSize(1);
        assertThat(result.totalAmount()).isEqualByComparingTo("19.98");
        verify(publisher).publishOrderCreated(any());
        verify(cartClient).clear(userId);
        assertThat(meterRegistry.get("orders_created_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void placeOrder_cartServiceDown_propagates503() {
        kycApproved();
        when(cartClient.getCart(userId)).thenThrow(new CartServiceUnavailableException());

        assertThatThrownBy(() -> service.placeOrder(userId))
                .isInstanceOf(CartServiceUnavailableException.class);
        verify(orderRepository, never()).createPending(any());
    }

    @Test
    void placeOrder_clearCartFails_orderStillPlaced() {
        kycApproved();
        when(cartClient.getCart(userId)).thenReturn(
                new CartSnapshot(List.of(new Line(productId, 1, new BigDecimal("5.00")))));
        when(orderRepository.createPending(any())).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("cart down")).when(cartClient).clear(userId);

        Order result = service.placeOrder(userId);

        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        verify(publisher).publishOrderCreated(any());
    }

    @Test
    void placeOrder_gatingOn_kycRejected_rejectsBeforeSaga() {
        when(kycStatus.findByUserId(userId)).thenReturn(
                Optional.of(new CustomerKycStatus(userId, KycStatus.REJECTED, NOW)));

        assertThatThrownBy(() -> service.placeOrder(userId)).isInstanceOf(KycNotApprovedException.class);
        verify(cartClient, never()).getCart(any());
        verify(orderRepository, never()).createPending(any());
        verify(publisher, never()).publishOrderCreated(any());
    }

    @Test
    void placeOrder_gatingOn_kycAbsent_failsClosed() {
        when(kycStatus.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.placeOrder(userId)).isInstanceOf(KycNotApprovedException.class);
        verify(cartClient, never()).getCart(any());
        verify(orderRepository, never()).createPending(any());
    }

    @Test
    void placeOrder_gatingOn_kycApproved_allows() {
        kycApproved();
        when(cartClient.getCart(userId)).thenReturn(
                new CartSnapshot(List.of(new Line(productId, 1, new BigDecimal("5.00")))));
        when(orderRepository.createPending(any())).thenAnswer(i -> i.getArgument(0));

        Order result = service.placeOrder(userId);

        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        verify(publisher).publishOrderCreated(any());
    }

    @Test
    void placeOrder_gatingOff_allowsRegardlessOfKyc() {
        service = serviceWithGating(false);
        when(cartClient.getCart(userId)).thenReturn(
                new CartSnapshot(List.of(new Line(productId, 1, new BigDecimal("5.00")))));
        when(orderRepository.createPending(any())).thenAnswer(i -> i.getArgument(0));

        Order result = service.placeOrder(userId);

        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        verify(publisher).publishOrderCreated(any());
    }

    @Test
    void getForUser_nonOwner_hiddenAsNotFound() {
        Order other = Order.create(UUID.randomUUID(), UUID.randomUUID(), "USD",
                List.of(), NOW);
        when(orderRepository.findById(other.id())).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.getForUser(other.id(), userId, false))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getForUser_admin_canSeeAnyOrder() {
        Order other = Order.create(UUID.randomUUID(), UUID.randomUUID(), "USD", List.of(), NOW);
        when(orderRepository.findById(other.id())).thenReturn(Optional.of(other));

        assertThat(service.getForUser(other.id(), userId, true)).isEqualTo(other);
    }
}
