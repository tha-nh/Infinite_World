package com.infinite.notification.contract.enumtype;

/**
 * Notification processing status
 */
public enum NotificationStatus {
    /**
     * Request accepted and queued for processing
     */
    ACCEPTED,
    
    /**
     * Notification is being processed
     */
    PROCESSING,
    
    /**
     * Notification delivered successfully
     */
    DELIVERED,
    
    /**
     * Notification partially delivered (some channels failed)
     */
    PARTIAL,
    
    /**
     * Notification delivery failed
     */
    FAILED,
    
    /**
     * Notification cancelled
     */
    CANCELLED
}
