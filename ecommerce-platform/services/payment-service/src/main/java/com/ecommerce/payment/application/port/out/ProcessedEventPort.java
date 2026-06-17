package com.ecommerce.payment.application.port.out;

import java.util.UUID;

public interface ProcessedEventPort {
    boolean firstSeen(UUID eventId);
}
