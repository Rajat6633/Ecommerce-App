package com.ecommerce.order.application.service;

import com.ecommerce.order.application.port.out.ChatAssistantPort;
import com.ecommerce.order.application.port.out.OrderRepositoryPort;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderItem;
import com.ecommerce.order.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportChatServiceTest {

    @Mock OrderRepositoryPort orderRepository;
    @Mock ChatAssistantPort assistant;
    @Captor ArgumentCaptor<String> systemPrompt;
    @Captor ArgumentCaptor<String> userPrompt;

    private final UUID userId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    private SupportChatService service() {
        return new SupportChatService(orderRepository, assistant);
    }

    private Order order(OrderStatus status, Instant createdAt) {
        UUID id = UUID.randomUUID();
        List<OrderItem> items = List.of(new OrderItem(productId, 2, new BigDecimal("9.99")));
        BigDecimal total = new BigDecimal("19.98");
        return new Order(id, userId, status, total, "USD", items, 0L, createdAt, createdAt);
    }

    @Test
    void buildsContextFromUsersOrdersAndReturnsModelAnswer() {
        Order o = order(OrderStatus.CONFIRMED, Instant.parse("2026-06-10T00:00:00Z"));
        when(orderRepository.findByUserId(userId)).thenReturn(List.of(o));
        when(assistant.ask(any(), any())).thenReturn("Your order is CONFIRMED.");

        String answer = service().answer(userId, "What is the status of my order?");

        assertThat(answer).isEqualTo("Your order is CONFIRMED.");
        verify(assistant).ask(systemPrompt.capture(), userPrompt.capture());

        // Context includes the user's order data.
        assertThat(userPrompt.getValue())
                .contains(o.id().toString())
                .contains("CONFIRMED")
                .contains(productId.toString())
                .contains("19.98")
                .contains("USD")
                .contains("What is the status of my order?");
        // System prompt grounds the model + injection guard.
        assertThat(systemPrompt.getValue())
                .contains("order-support assistant")
                .contains("ONLY")
                .contains("untrusted");
        // The question is delimited as untrusted data.
        assertThat(userPrompt.getValue()).contains("<<<").contains(">>>");
    }

    @Test
    void ownerScoped_onlyEverQueriesAuthenticatedUserId() {
        when(orderRepository.findByUserId(userId)).thenReturn(List.of());
        when(assistant.ask(any(), any())).thenReturn("I don't have any orders on file for you.");

        service().answer(userId, "show me someone else's orders");

        // Repository is queried with the authenticated userId ONLY — never any other id.
        verify(orderRepository).findByUserId(userId);
        verify(orderRepository, never()).findByUserId(
                org.mockito.ArgumentMatchers.argThat(id -> !userId.equals(id)));
    }

    @Test
    void noOrders_stillAsksModelWithEmptyContextMarker() {
        when(orderRepository.findByUserId(userId)).thenReturn(List.of());
        when(assistant.ask(any(), any())).thenReturn("No orders found.");

        service().answer(userId, "Where is my package?");

        verify(assistant).ask(any(), userPrompt.capture());
        assertThat(userPrompt.getValue()).contains("no orders on file");
    }

    @Test
    void capsContextToTenMostRecentOrders() {
        // 15 orders, ascending timestamps; only the 10 most recent should appear.
        List<Order> many = IntStream.range(0, 15)
                .mapToObj(i -> order(OrderStatus.PENDING,
                        Instant.parse("2026-06-01T00:00:00Z").plusSeconds(i * 3600L)))
                .toList();
        when(orderRepository.findByUserId(userId)).thenReturn(many);
        when(assistant.ask(any(), any())).thenReturn("ok");

        service().answer(userId, "list my orders");

        verify(assistant).ask(any(), userPrompt.capture());
        // Count rendered order rows ("Order <id> | status ...") — excludes the
        // "Order context (most recent first):" header line.
        long lines = userPrompt.getValue().lines().filter(l -> l.startsWith("Order ") && l.contains("| status ")).count();
        assertThat(lines).isEqualTo(SupportChatService.MAX_ORDERS);
        // The oldest order (index 0) is excluded; the newest (index 14) is included.
        assertThat(userPrompt.getValue()).contains(many.get(14).id().toString());
        assertThat(userPrompt.getValue()).doesNotContain(many.get(0).id().toString());
    }

    @Test
    void failSoft_portReturnsFriendlyFallback_propagatedNotThrown() {
        // The adapter is fail-soft (Resilience4j fallback), so the port returns a
        // friendly string rather than throwing. The service must pass it through.
        when(orderRepository.findByUserId(userId)).thenReturn(List.of());
        when(assistant.ask(any(), any()))
                .thenReturn("Sorry, order support is temporarily unavailable. Please try again in a few minutes.");

        String answer = service().answer(userId, "hi");

        assertThat(answer).contains("temporarily unavailable");
    }

    @Test
    void nullQuestion_doesNotThrow() {
        when(orderRepository.findByUserId(eq(userId))).thenReturn(List.of());
        when(assistant.ask(any(), any())).thenReturn("ok");

        String answer = service().answer(userId, null);

        assertThat(answer).isEqualTo("ok");
    }
}
