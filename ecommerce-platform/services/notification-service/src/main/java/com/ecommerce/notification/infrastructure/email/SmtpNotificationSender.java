package com.ecommerce.notification.infrastructure.email;

import com.ecommerce.notification.application.port.out.NotificationSenderPort;
import com.ecommerce.notification.domain.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Real email delivery over SMTP, active when {@code notification.mail.enabled=true}.
 * Spring Boot autoconfigures {@link JavaMailSender} from {@code spring.mail.*}.
 * Delivery failures are caught and reported as a FAILED result (never thrown)
 * so the consumer commits a FAILED audit row rather than retrying forever.
 */
@Component
@ConditionalOnProperty(name = "notification.mail.enabled", havingValue = "true")
public class SmtpNotificationSender implements NotificationSenderPort {

    private static final Logger log = LoggerFactory.getLogger(SmtpNotificationSender.class);

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpNotificationSender(JavaMailSender mailSender,
                                  @Value("${notification.mail.from:no-reply@ecommerce.local}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public SendResult send(Notification notification) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(notification.recipient());
            message.setSubject(subjectFor(notification));
            message.setText(notification.payload());
            mailSender.send(message);
            return SendResult.success();
        } catch (MailException ex) {
            log.warn("SMTP delivery failed to {}: {}", notification.recipient(), ex.getMessage());
            return SendResult.failure(ex.getMessage());
        }
    }

    private static String subjectFor(Notification notification) {
        return switch (notification.type()) {
            case ORDER_CONFIRMED -> "Your order is confirmed";
            case PAYMENT_COMPLETED -> "Payment received";
        };
    }
}
