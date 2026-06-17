package com.ecommerce.payment.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

/** Abstraction over the payment processor (simulated here). */
public interface PaymentGatewayPort {

    ChargeResult charge(UUID orderId, BigDecimal amount, String currency);

    record ChargeResult(boolean approved, String declineReason) {
        public static ChargeResult approve() {
            return new ChargeResult(true, null);
        }
        public static ChargeResult decline(String reason) {
            return new ChargeResult(false, reason);
        }
    }
}
