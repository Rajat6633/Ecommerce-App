package com.ecommerce.notification.infrastructure.email;

import com.ecommerce.notification.application.port.out.NotificationSenderPort;
import com.ecommerce.notification.domain.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default sender: logs the message instead of contacting an SMTP server. Active
 * unless {@code notification.mail.enabled=true}, so local/dev/test runs never
 * depend on a mail server. Always reports success.
 */
@Component
@ConditionalOnProperty(name = "notification.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingNotificationSender implements NotificationSenderPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public SendResult send(Notification notification) {
        log.info("[SIMULATED-EMAIL] to={} type={} reference={} body=\"{}\"",
                notification.recipient(), notification.type(), notification.referenceId(),
                notification.payload());
        return SendResult.success();
    }
}
