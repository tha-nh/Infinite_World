package com.infinite.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infinite.notification.dto.event.SmsNotificationEvent;
import com.infinite.notification.dto.request.SmsRequest;
import com.infinite.notification.messaging.MessageConsumer;
import com.infinite.notification.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for SMS notifications
 * Only active when messaging.provider=kafka
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.provider", havingValue = "kafka", matchIfMissing = true)
public class SmsNotificationConsumer implements MessageConsumer {
    
    private final SmsService smsService;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(
        topics = "${messaging.topics.sms:notification.sms}",
        groupId = "${spring.kafka.consumer.group-id:notification-service}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
        @Payload Object payload,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment
    ) {
        try {
            log.debug("Received SMS notification from topic: {}, partition: {}, offset: {}", 
                topic, partition, offset);
            
            // Convert payload to SmsNotificationEvent
            SmsNotificationEvent event = objectMapper.convertValue(payload, SmsNotificationEvent.class);
            
            // Check if this is OTP SMS (has metadata with otp and type)
            if (event.getMetadata() != null && event.getMetadata().containsKey("otp")) {
                String otp = (String) event.getMetadata().get("otp");
                String type = (String) event.getMetadata().get("type");
                smsService.sendOtpSms(event.getPhoneNumber(), otp, type);
            } else {
                // Build SmsRequest from event
                SmsRequest request = SmsRequest.builder()
                    .phoneNumber(event.getPhoneNumber())
                    .message(event.getMessage())
                    .template(event.getTemplate())
                    .variables(event.getVariables())
                    .build();
                
                // Send SMS
                smsService.sendSms(request);
            }
            
            // Acknowledge message
            acknowledgment.acknowledge();
            log.info("SMS sent successfully to: {}", event.getPhoneNumber());
            
        } catch (Exception e) {
            log.error("Failed to process SMS notification from topic: {}, partition: {}, offset: {}", 
                topic, partition, offset, e);
            // Don't acknowledge - message will be redelivered
        }
    }
}
