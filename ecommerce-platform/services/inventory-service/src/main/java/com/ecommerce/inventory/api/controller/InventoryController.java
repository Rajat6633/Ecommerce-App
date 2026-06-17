package com.ecommerce.inventory.api.controller;

import com.ecommerce.inventory.api.dto.InventoryResponse;
import com.ecommerce.inventory.api.dto.ReceiveStockRequest;
import com.ecommerce.inventory.api.dto.UpsertStockRequest;
import com.ecommerce.inventory.application.port.in.InventoryAdminUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory", description = "Stock levels and admin adjustments")
public class InventoryController {

    private final InventoryAdminUseCase inventory;

    public InventoryController(InventoryAdminUseCase inventory) {
        this.inventory = inventory;
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get stock for a product", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<InventoryResponse> get(@PathVariable UUID productId) {
        return ResponseEntity.ok(InventoryResponse.from(inventory.getByProduct(productId)));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set stock for a product (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<InventoryResponse> upsert(@PathVariable UUID productId,
                                                    @Valid @RequestBody UpsertStockRequest req) {
        return ResponseEntity.ok(InventoryResponse.from(
                inventory.upsertStock(productId, req.onHand(), req.reorderLevel())));
    }

    @PostMapping("/{productId}/receive")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Receive (add) stock (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<InventoryResponse> receive(@PathVariable UUID productId,
                                                     @Valid @RequestBody ReceiveStockRequest req) {
        return ResponseEntity.ok(InventoryResponse.from(
                inventory.receiveStock(productId, req.quantity())));
    }
}
