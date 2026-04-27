package com.infinite.notification.consumer;

import com.infinite.common.dto.event.AccountVerificationEvent;
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
 * Kafka consumer for account verification notifications
 * 
 * @deprecated This consumer is deprecated. Use EmailNotificationConsumer with EmailType.REGISTRATION_VERIFICATION instead.
 * Will be removed in a future version after migration is complete.
 */
@Deprecated(since = "2.0", forRemoval = true)
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.provider", havingValue = "kafka", matchIfMissing = true)
public class AccountVerificationConsumer implements MessageConsumer {
    
    private final EmailService emailService;
    
    @KafkaListener(
        topics = "${messaging.topics.account-verification:account.verification}",
        groupId = "${spring.kafka.consumer.group-id:notification-service}",
        containerFactory = "accountVerificationListenerContainerFactory"
    )
    public void consume(
        @Payload AccountVerificationEvent event,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment
    ) {
        try {
            log.debug("Received account verification from topic: {}, partition: {}, offset: {}", 
                topic, partition, offset);
            
            // Set locale from event
            Locale locale = Locale.forLanguageTag(event.getLocale() != null ? event.getLocale() : "en");
            LocaleContextHolder.setLocale(locale);
            log.debug("Set locale to: {}", locale);
            
            // Send account verification email with confirmation links
            emailService.sendAccountVerificationEmail(
                event.getTo(), 
                event.getUsername(),
                event.getVerificationToken(),
                event.getUserId()
            );
            
            // Acknowledge message
            acknowledgment.acknowledge();
            log.info("Account verification email sent successfully to: {}", event.getTo());
            
        } catch (Exception e) {
            log.error("Failed to process account verification from topic: {}, partition: {}, offset: {}", 
                topic, partition, offset, e);
            // Don't acknowledge - message will be redelivered
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }
}