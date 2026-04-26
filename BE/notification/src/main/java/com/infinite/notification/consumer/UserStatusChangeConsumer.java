package com.infinite.notification.consumer;

import com.infinite.common.dto.event.UserStatusChangeEvent;
import com.infinite.notification.messaging.MessageConsumer;
import com.infinite.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Kafka consumer for user status change notifications
 * 
 * @deprecated This consumer is deprecated. Use EmailNotificationConsumer with appropriate EmailType instead.
 * Will be removed in a future version after migration is complete.
 */
@Deprecated(since = "2.0", forRemoval = true)
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.provider", havingValue = "kafka", matchIfMissing = true)
public class UserStatusChangeConsumer implements MessageConsumer {
    
    private final EmailService emailService;
    
    @KafkaListener(
        topics = "${messaging.topics.user-status-change:user.status.change}",
        groupId = "${spring.kafka.consumer.group-id:notification-service}",
        containerFactory = "userStatusChangeListenerContainerFactory"
    )
    public void consume(
        @Payload UserStatusChangeEvent event,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment
    ) {
        try {
            log.debug("Received user status change from topic: {}, partition: {}, offset: {}", 
                topic, partition, offset);
            
            // Set locale from event
            Locale locale = Locale.forLanguageTag(event.getLocale() != null ? event.getLocale() : "en");
            LocaleContextHolder.setLocale(locale);
            log.debug("Set locale to: {}", locale);
            
            // Send appropriate notification based on action
            switch (event.getAction()) {
                case "LOCKED":
                    emailService.sendUserLockedEmail(
                        event.getTo(),
                        event.getUsername(),
                        event.getLockTime(),
                        event.getPerformedBy()
                    );
                    break;
                case "UNLOCKED":
                    emailService.sendUserUnlockedEmail(
                        event.getTo(),
                        event.getUsername(),
                        event.getPerformedBy()
                    );
                    break;
                case "AUTO_UNLOCKED":
                    emailService.sendUserAutoUnlockedEmail(
                        event.getTo(),
                        event.getUsername()
                    );
                    break;
                case "UPDATED":
                    emailService.sendUserUpdatedEmail(
                        event.getTo(),
                        event.getUsername(),
                        event.getPerformedBy()
                    );
                    break;
                default:
                    log.warn("Unknown user status change action: {}", event.getAction());
            }
            
            // Acknowledge message
            acknowledgment.acknowledge();
            log.info("User status change email sent successfully to: {} for action: {}", 
                event.getTo(), event.getAction());
            
        } catch (Exception e) {
            log.error("Failed to process user status change from topic: {}, partition: {}, offset: {}", 
                topic, partition, offset, e);
            // Don't acknowledge - message will be redelivered
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }
}