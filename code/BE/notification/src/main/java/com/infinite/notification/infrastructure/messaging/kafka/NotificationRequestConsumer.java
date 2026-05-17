package com.infinite.notification.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infinite.notification.dto.event.NotificationDlqEvent;
import com.infinite.notification.infrastructure.observability.NotificationMetrics;
import com.infinite.notification.application.command.CreateNotificationRequestUseCase;
import com.infinite.notification.contract.event.NotificationRequestEvent;
import com.infinite.notification.messaging.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRequestConsumer {

    private final CreateNotificationRequestUseCase createNotificationRequestUseCase;
    private final ObjectMapper objectMapper;
    private final MessagePublisher messagePublisher;
    private final NotificationMetrics metrics;

    @Value("${messaging.topics.notification-dlq:notification.dlq}")
    private String dlqTopic;

    @KafkaListener(
        topics = "${messaging.topics.notification-request:notification.request.v1}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consumeNotificationRequest(String message, Acknowledgment acknowledgment) {
        NotificationRequestEvent event;
        try {
            log.info("Received notification request from Kafka: {}", message);
            event = objectMapper.readValue(message, NotificationRequestEvent.class);
        } catch (Exception e) {
            log.error("Invalid notification request payload, send to DLQ", e);
            publishDlq(message, null, "request-deserialize", e);
            acknowledgment.acknowledge();
            return;
        }

        try {
            createNotificationRequestUseCase.execute(event);
            acknowledgment.acknowledge();
            metrics.increment("request.kafka.processed");
            log.info("Successfully processed notification request: eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to process notification request from Kafka", e);
            metrics.increment("request.kafka.failed");
            // Do not acknowledge retryable failures. Kafka will redeliver according to container policy.
        }
    }

    private void publishDlq(String payload, String sourceKey, String failureStage, Exception ex) {
        NotificationDlqEvent dlqEvent = NotificationDlqEvent.builder()
                .sourceTopic("notification.request.v1")
                .sourceKey(sourceKey)
                .failureStage(failureStage)
                .payload(payload)
                .errorMessage(ex.getMessage())
                .retryCount(0)
                .occurredAt(Instant.now())
                .build();
        messagePublisher.publish(dlqTopic, sourceKey != null ? sourceKey : UUID.randomUUID().toString(), dlqEvent);
        metrics.increment("dlq.published", "stage", failureStage);
    }
}
