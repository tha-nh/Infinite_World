package com.infinite.notification.contract.enumtype;

/**
 * Notification types for categorization
 */
public enum NotificationType {
    /**
     * System notifications (maintenance, updates, etc.)
     */
    SYSTEM,
    
    /**
     * Account related notifications (verification, password reset, etc.)
     */
    ACCOUNT,
    
    /**
     * Game related notifications (events, rewards, etc.)
     */
    GAME,
    
    /**
     * Payment related notifications (transactions, invoices, etc.)
     */
    PAYMENT,
    
    /**
     * Social notifications (friend requests, messages, etc.)
     */
    SOCIAL,
    
    /**
     * Promotional notifications (offers, campaigns, etc.)
     */
    PROMOTION,
    
    /**
     * General announcements
     */
    ANNOUNCEMENT
}
