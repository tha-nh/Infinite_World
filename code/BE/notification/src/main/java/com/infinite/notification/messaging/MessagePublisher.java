package com.infinite.notification.messaging;

/**
 * Abstraction layer for message publishing
 * Allows switching between Kafka, RabbitMQ, Redis, etc. without changing business logic
 */
public interface MessagePublisher {
    
    /**
     * Publish a message to a topic/queue
     * @param topic The destination topic/queue name
     * @param message The message payload
     */
    void publish(String topic, Object message);
    
    /**
     * Publish a message with a specific key (for partitioning)
     * @param topic The destination topic/queue name
     * @param key The message key (used for partitioning in Kafka)
     * @param message The message payload
     */
    void publish(String topic, String key, Object message);
}
