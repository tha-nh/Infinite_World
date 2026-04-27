package com.infinite.notification.contract.constant;

/**
 * Public topic names for notification integration
 * These are the only topics external services should use
 */
public final class NotificationTopicNames {
    
    private NotificationTopicNames() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Topic for notification requests (v1)
     * Services publish NotificationRequestEvent to this topic
     */
    public static final String NOTIFICATION_REQUEST_V1 = "notification.request.v1";
    
    /**
     * Topic for notification status changes (v1)
     * Notification service publishes status updates to this topic
     */
    public static final String NOTIFICATION_STATUS_CHANGED_V1 = "notification.status.changed.v1";
}
