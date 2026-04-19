package com.infinite.notification.messaging.kafka;

import com.infinite.notification.messaging.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka implementation of MessagePublisher
 * Only active when messaging.provider=kafka
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.provider", havingValue = "kafka", matchIfMissing = true)
public class KafkaMessagePublisher implements MessagePublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public void publish(String topic, Object message) {
        publish(topic, null, message);
    }
    
    @Override
    public void publish(String topic, String key, Object message) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, message);
        
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send message to topic: {}, key: {}", topic, key, ex);
            } else {
                log.debug("Message sent successfully to topic: {}, partition: {}, offset: {}", 
                    topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }
}
