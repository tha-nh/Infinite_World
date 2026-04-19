package com.infinite.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Only active when messaging.provider=kafka
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.provider", havingValue = "kafka", matchIfMissing = true)
public class EmailNotificationConsumer implements MessageConsumer {
    
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    
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
            log.debug("Received email notification from topic: {}, partition: {}, offset: {}", 
                topic, partition, offset);
            
            // Set locale from event
            Locale locale = Locale.forLanguageTag(event.getLocale() != null ? event.getLocale() : "en");
            LocaleContextHolder.setLocale(locale);
            log.debug("Set locale to: {}", locale);
            
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
            // Regular email
            else {
                // Build EmailRequest from event
                EmailRequest request = EmailRequest.builder()
                    .to(event.getTo())
                    .subject(event.getSubject())
                    .content(event.getContent())
                    .template(event.getTemplate())
                    .variables(event.getVariables())
                    .isHtml(event.isHtml())
                    .build();
                
                // Send email
                emailService.sendEmail(request);
            }
            
            // Acknowledge message
            acknowledgment.acknowledge();
            log.info("Email sent successfully to: {}", event.getTo());
            
        } catch (Exception e) {
            log.error("Failed to process email notification from topic: {}, partition: {}, offset: {}", 
                topic, partition, offset, e);
            // Don't acknowledge - message will be redelivered
            // TODO: Implement DLQ (Dead Letter Queue) for failed messages after max retries
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }
}
