package com.infinite.notification.consumer;

import com.infinite.common.dto.event.EmailNotificationEvent;
import com.infinite.notification.dto.request.EmailRequest;
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
 * Kafka consumer for email notifications
 * Supports both new format (with emailType) and legacy format (without emailType) for backward compatibility
 * Only active when messaging.provider=kafka
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.provider", havingValue = "kafka", matchIfMissing = true)
public class EmailNotificationConsumer implements MessageConsumer {
    
    private final EmailService emailService;
    
    @KafkaListener(
        topics = "${messaging.topics.email:notification.email}",
        groupId = "${spring.kafka.consumer.group-id:notification-service}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
        @Payload EmailNotificationEvent event,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment
    ) {
        try {
            log.debug("Received email notification from topic: {}, partition: {}, offset: {}, emailType: {}", 
                topic, partition, offset, event.getEmailType());
            
            // Set locale from event
            Locale locale = Locale.forLanguageTag(event.getLocale() != null ? event.getLocale() : "en");
            LocaleContextHolder.setLocale(locale);
            log.debug("Set locale to: {}", locale);
            
            // Check if this is new format (with emailType) or old format (without emailType)
            if (event.getEmailType() != null) {
                // NEW FORMAT: Use unified templated approach
                emailService.sendTemplatedEmail(event);
            } else {
                // OLD FORMAT: Fallback to legacy handling for backward compatibility
                log.warn("Received email event without emailType (legacy format), using fallback handling");
                handleLegacyEmailEvent(event);
            }
            
            // Acknowledge message
            acknowledgment.acknowledge();
            log.info("Email sent successfully to: {} with type: {}", event.getTo(), event.getEmailType());
            
        } catch (Exception e) {
            log.error("Failed to process email notification from topic: {}, partition: {}, offset: {}", 
                topic, partition, offset, e);
            // Don't acknowledge - message will be redelivered
            // TODO: Implement DLQ (Dead Letter Queue) for failed messages after max retries
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }
    
    /**
     * Handle legacy email events that don't have emailType (backward compatibility)
     */
    private void handleLegacyEmailEvent(EmailNotificationEvent event) {
        // Check if this is OTP email (has metadata with otp and type)
        if (event.getMetadata() != null && event.getMetadata().containsKey("otp")) {
            String otp = (String) event.getMetadata().get("otp");
            String type = (String) event.getMetadata().get("type");
            emailService.sendOtpEmail(event.getTo(), otp, type);
        } 
        // Check if this is password reset verification email
        else if (event.getMetadata() != null && event.getMetadata().containsKey("defaultPassword")) {
            String verificationUrl = (String) event.getMetadata().get("verificationUrl");
            String defaultPassword = (String) event.getMetadata().get("defaultPassword");
            emailService.sendPasswordResetVerificationEmail(event.getTo(), verificationUrl, defaultPassword);
        }
        // Regular email with subject and content
        else if (event.getSubject() != null && event.getContent() != null) {
            EmailRequest request = EmailRequest.builder()
                .to(event.getTo())
                .subject(event.getSubject())
                .content(event.getContent())
                .isHtml(event.isHtml())
                .build();
            emailService.sendEmail(request);
        } else {
            log.error("Unable to handle legacy email event - missing required fields");
            throw new IllegalArgumentException("Legacy email event missing required fields");
        }
    }
}
