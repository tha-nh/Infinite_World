package com.infinite.notification.contract.enumtype;

/**
 * Notification delivery channels
 */
public enum NotificationChannel {
    /**
     * Store notification in user inbox
     */
    INBOX,
    
    /**
     * Push realtime notification via WebSocket
     */
    REALTIME,
    
    /**
     * Send notification via email
     */
    EMAIL
}
