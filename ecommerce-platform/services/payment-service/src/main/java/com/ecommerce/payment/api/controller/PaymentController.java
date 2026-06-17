package com.ecommerce.payment.api.controller;

import com.ecommerce.payment.api.dto.PaymentResponse;
import com.ecommerce.payment.application.port.in.PaymentQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Payment status lookup")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentQueryUseCase payments;

    public PaymentController(PaymentQueryUseCase payments) {
        this.payments = payments;
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get the payment for an order")
    public PaymentResponse getByOrder(@PathVariable UUID orderId) {
        return PaymentResponse.from(payments.getByOrderId(orderId));
    }
}
