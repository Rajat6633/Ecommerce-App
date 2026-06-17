package com.ecommerce.order.api.controller;

import com.ecommerce.order.api.dto.OrderResponse;
import com.ecommerce.order.api.dto.OrderStatusResponse;
import com.ecommerce.order.api.dto.SupportChatRequest;
import com.ecommerce.order.api.dto.SupportChatResponse;
import com.ecommerce.order.application.port.in.OrderQueryUseCase;
import com.ecommerce.order.application.port.in.PlaceOrderUseCase;
import com.ecommerce.order.application.port.in.SupportChatUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Place orders, view history and status")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final PlaceOrderUseCase placeOrder;
    private final OrderQueryUseCase query;
    private final SupportChatUseCase supportChat;

    public OrderController(PlaceOrderUseCase placeOrder, OrderQueryUseCase query,
                           SupportChatUseCase supportChat) {
        this.placeOrder = placeOrder;
        this.query = query;
        this.supportChat = supportChat;
    }

    @PostMapping
    @Operation(summary = "Place an order from the current user's cart")
    public ResponseEntity<OrderResponse> place(Authentication auth) {
        var order = placeOrder.placeOrder(userId(auth));
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @GetMapping
    @Operation(summary = "List the current user's orders")
    public List<OrderResponse> history(Authentication auth) {
        return query.history(userId(auth)).stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an order (owner or ADMIN)")
    public OrderResponse get(@PathVariable UUID id, Authentication auth) {
        return OrderResponse.from(query.getForUser(id, userId(auth), isAdmin(auth)));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Get an order's status")
    public OrderStatusResponse status(@PathVariable UUID id, Authentication auth) {
        return OrderStatusResponse.from(query.getForUser(id, userId(auth), isAdmin(auth)));
    }

    @PostMapping("/support/chat")
    @Operation(summary = "Ask a natural-language question about YOUR orders (AI support chatbot)")
    public SupportChatResponse supportChat(@Valid @RequestBody SupportChatRequest request,
                                           Authentication auth) {
        // Owner-scoped: userId comes from the JWT subject, never from the body, so a
        // user can only ever ask about their OWN orders.
        return new SupportChatResponse(supportChat.answer(userId(auth), request.question()));
    }

    private static UUID userId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }

    private static boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
