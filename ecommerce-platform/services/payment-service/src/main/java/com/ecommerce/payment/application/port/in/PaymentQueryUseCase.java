package com.ecommerce.payment.application.port.in;

import com.ecommerce.payment.domain.model.Payment;

import java.util.UUID;

public interface PaymentQueryUseCase {
    Payment getByOrderId(UUID orderId);
}
