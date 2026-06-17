package com.ecommerce.payment.application.port.out;

import com.ecommerce.payment.domain.model.Payment;

import java.util.UUID;

public interface PaymentEventPublisherPort {

    void publishCompleted(Payment payment);

    void publishFailed(UUID orderId, String reason);
}
