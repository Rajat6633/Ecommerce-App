package com.ecommerce.notification.api.controller;

import com.ecommerce.notification.api.dto.NotificationResponse;
import com.ecommerce.notification.application.port.in.NotificationQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Notification audit-log lookup")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationQueryUseCase notifications;

    public NotificationController(NotificationQueryUseCase notifications) {
        this.notifications = notifications;
    }

    @GetMapping("/{referenceId}")
    @Operation(summary = "List notifications raised for a reference (e.g. an order)")
    public List<NotificationResponse> getByReference(@PathVariable UUID referenceId) {
        return notifications.getByReference(referenceId).stream()
                .map(NotificationResponse::from)
                .toList();
    }
}
