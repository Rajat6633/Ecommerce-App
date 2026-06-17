package com.ecommerce.cart.api.controller;

import com.ecommerce.cart.api.dto.AddItemRequest;
import com.ecommerce.cart.api.dto.CartResponse;
import com.ecommerce.cart.api.dto.UpdateQuantityRequest;
import com.ecommerce.cart.application.port.in.CartUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "Per-user shopping cart")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartUseCase cart;

    public CartController(CartUseCase cart) {
        this.cart = cart;
    }

    @GetMapping
    @Operation(summary = "View the current user's cart")
    public CartResponse view(@AuthenticationPrincipal Jwt jwt) {
        return CartResponse.from(cart.getCart(userId(jwt)));
    }

    @PostMapping("/items")
    @Operation(summary = "Add a product to the cart")
    public CartResponse addItem(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddItemRequest req) {
        return CartResponse.from(cart.addItem(userId(jwt), req.productId(), req.quantity()));
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Update a line quantity (0 removes it)")
    public CartResponse updateItem(@AuthenticationPrincipal Jwt jwt,
                                   @PathVariable UUID productId,
                                   @Valid @RequestBody UpdateQuantityRequest req) {
        return CartResponse.from(cart.updateItem(userId(jwt), productId, req.quantity()));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove a product from the cart")
    public CartResponse removeItem(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID productId) {
        return CartResponse.from(cart.removeItem(userId(jwt), productId));
    }

    @DeleteMapping
    @Operation(summary = "Empty the cart")
    public ResponseEntity<Void> clear(@AuthenticationPrincipal Jwt jwt) {
        cart.clearCart(userId(jwt));
        return ResponseEntity.noContent().build();
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
