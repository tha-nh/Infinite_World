package com.infinite.notification.messaging;

/**
 * Marker interface for message consumers
 * Implementations will use provider-specific annotations (@KafkaListener, @RabbitListener, etc.)
 */
public interface MessageConsumer {
    // Marker interface - actual methods defined in implementations
}
