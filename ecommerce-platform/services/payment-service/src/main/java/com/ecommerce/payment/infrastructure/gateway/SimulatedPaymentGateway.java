package com.ecommerce.payment.infrastructure.gateway;

import com.ecommerce.payment.application.port.out.PaymentGatewayPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulated payment processor. Approves a configurable fraction of charges
 * (payment.success-rate); declines invalid amounts outright. Replace with a
 * real PSP adapter (Stripe/Adyen/etc.) without touching the application layer.
 */
@Component
public class SimulatedPaymentGateway implements PaymentGatewayPort {

    private final double successRate;

    public SimulatedPaymentGateway(@Value("${payment.success-rate:0.9}") double successRate) {
        this.successRate = successRate;
    }

    @Override
    public ChargeResult charge(UUID orderId, BigDecimal amount, String currency) {
        if (amount == null || amount.signum() <= 0) {
            return ChargeResult.decline("INVALID_AMOUNT");
        }
        boolean approved = ThreadLocalRandom.current().nextDouble() < successRate;
        return approved ? ChargeResult.approve() : ChargeResult.decline("PAYMENT_DECLINED");
    }
}
