package com.infinite.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infinite.notification.dto.event.WebSocketNotificationEvent;
import com.infinite.notification.messaging.MessageConsumer;
import com.infinite.notification.service.WebSocketNotificationService;
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
 * Kafka consumer for WebSocket notifications
 * Only active when messaging.provider=kafka
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.provider", havingValue = "kafka", matchIfMissing = true)
public class WebSocketNotificationConsumer implements MessageConsumer {
    
    private final WebSocketNotificationService webSocketService;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(
        topics = "${messaging.topics.websocket:notification.websocket}",
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
            log.debug("Received WebSocket notification from topic: {}, partition: {}, offset: {}", 
                topic, partition, offset);
            
            // Convert payload to WebSocketNotificationEvent
            WebSocketNotificationEvent event = objectMapper.convertValue(payload, WebSocketNotificationEvent.class);
            
            // Send WebSocket notification
            if (event.isBroadcast()) {
                webSocketService.broadcast(event.getType(), event.getTitle(), event.getMessage());
            } else {
                webSocketService.sendToUser(event.getUserId(), event.getType(), 
                    event.getTitle(), event.getMessage());
            }
            
            // Acknowledge message
            acknowledgment.acknowledge();
            log.info("WebSocket notification sent successfully");
            
        } catch (Exception e) {
            log.error("Failed to process WebSocket notification from topic: {}, partition: {}, offset: {}", 
                topic, partition, offset, e);
            // Don't acknowledge - message will be redelivered
        }
    }
}
