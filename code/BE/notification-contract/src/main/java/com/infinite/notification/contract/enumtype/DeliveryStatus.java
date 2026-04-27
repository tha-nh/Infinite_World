package com.infinite.notification.contract.enumtype;

/**
 * Delivery status for individual channels
 */
public enum DeliveryStatus {
    /**
     * Delivery pending
     */
    PENDING,
    
    /**
     * Delivery in progress
     */
    SENDING,
    
    /**
     * Successfully sent to provider
     */
    SENT,
    
    /**
     * Confirmed delivered to recipient
     */
    DELIVERED,
    
    /**
     * Delivery failed
     */
    FAILED,
    
    /**
     * Delivery bounced
     */
    BOUNCED,
    
    /**
     * Delivery skipped (e.g., user preference)
     */
    SKIPPED
}
