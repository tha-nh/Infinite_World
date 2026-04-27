package com.infinite.notification.contract.client;

import com.infinite.notification.contract.event.NotificationRequestEvent;

/**
 * Interface for publishing notification requests
 * Services should implement this to send notifications
 */
public interface NotificationPublisher {
    
    /**
     * Publish a notification request
     * 
     * @param event The notification request event
     */
    void publishNotificationRequest(NotificationRequestEvent event);
}
