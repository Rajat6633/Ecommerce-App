package com.ecommerce.order.application.service;

import com.ecommerce.order.application.port.in.OrderQueryUseCase;
import com.ecommerce.order.application.port.in.PlaceOrderUseCase;
import com.ecommerce.order.application.port.out.CartClientPort;
import com.ecommerce.order.application.port.out.CartClientPort.CartSnapshot;
import com.ecommerce.order.application.port.out.CustomerKycStatusPort;
import com.ecommerce.order.application.port.out.OrderEventPublisherPort;
import com.ecommerce.order.application.port.out.OrderRepositoryPort;
import com.ecommerce.order.domain.exception.EmptyCartException;
import com.ecommerce.order.domain.exception.KycNotApprovedException;
import com.ecommerce.order.domain.exception.OrderNotFoundException;
import com.ecommerce.order.domain.model.CustomerKycStatus;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderItem;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Command + query side of orders. {@code placeOrder} snapshots the cart, persists
 * the order (committed inside the repository adapter), then publishes
 * {@code order.created} — so the event is emitted only after the order is durably
 * stored. Cart clearing is best-effort.
 */
@Service
public class OrderService implements PlaceOrderUseCase, OrderQueryUseCase {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepositoryPort orderRepository;
    private final CartClientPort cartClient;
    private final OrderEventPublisherPort publisher;
    private final CustomerKycStatusPort kycStatus;
    private final OrderKycProperties kycProperties;
    private final Clock clock;
    private final String defaultCurrency;
    private final Counter ordersCreated;

    public OrderService(OrderRepositoryPort orderRepository,
                        CartClientPort cartClient,
                        OrderEventPublisherPort publisher,
                        CustomerKycStatusPort kycStatus,
                        OrderKycProperties kycProperties,
                        Clock clock,
                        MeterRegistry meterRegistry,
                        @Value("${order.default-currency:USD}") String defaultCurrency) {
        this.orderRepository = orderRepository;
        this.cartClient = cartClient;
        this.publisher = publisher;
        this.kycStatus = kycStatus;
        this.kycProperties = kycProperties;
        this.clock = clock;
        this.defaultCurrency = defaultCurrency;
        this.ordersCreated = Counter.builder("orders_created_total")
                .description("Total orders placed").register(meterRegistry);
    }

    @Override
    public Order placeOrder(UUID userId) {
        requireKycApproved(userId);   // precondition: gate checkout before the saga starts
        CartSnapshot cart = cartClient.getCart(userId);
        if (cart.isEmpty()) {
            throw new EmptyCartException();
        }
        List<OrderItem> items = cart.items().stream()
                .map(l -> new OrderItem(l.productId(), l.quantity(), l.unitPrice()))
                .toList();
        Order order = Order.create(UUID.randomUUID(), userId, defaultCurrency, items, clock.instant());

        Order saved = orderRepository.createPending(order);   // commits here
        publisher.publishOrderCreated(saved);                 // then publish (post-commit)
        ordersCreated.increment();

        try {
            cartClient.clear(userId);                          // best-effort
        } catch (RuntimeException e) {
            log.warn("Failed to clear cart for user {} after placing order {}: {}",
                    userId, saved.id(), e.toString());
        }
        log.info("Order placed orderId={} userId={} total={}", saved.id(), userId, saved.totalAmount());
        return saved;
    }

    /**
     * Checkout KYC gate, evaluated before the order saga starts. Reads the local
     * read-model only (no synchronous call to kyc-service). Fail-closed: when
     * gating is enabled, an absent or non-APPROVED status rejects the order.
     * When gating is disabled the gate is observe-only and never blocks.
     */
    private void requireKycApproved(UUID userId) {
        boolean approved = kycStatus.findByUserId(userId)
                .map(CustomerKycStatus::isApproved)
                .orElse(false);
        if (!kycProperties.enabled()) {
            if (!approved) {
                log.info("KYC gating disabled (observe-only): would have blocked userId={}", userId);
            }
            return;
        }
        if (!approved) {
            log.warn("Order rejected: KYC not approved userId={}", userId);
            throw new KycNotApprovedException();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> history(UUID userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getForUser(UUID orderId, UUID requesterId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId.toString()));
        if (!isAdmin && !order.userId().equals(requesterId)) {
            // hide existence from non-owners
            throw new OrderNotFoundException(orderId.toString());
        }
        return order;
    }
}
