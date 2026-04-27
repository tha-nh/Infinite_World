package com.infinite.notification.contract.enumtype;

/**
 * Notification priority levels
 */
public enum NotificationPriority {
    /**
     * Low priority - can be batched and delayed
     */
    LOW(0),
    
    /**
     * Normal priority - standard delivery
     */
    NORMAL(1),
    
    /**
     * High priority - should be delivered quickly
     */
    HIGH(2),
    
    /**
     * Urgent priority - immediate delivery required
     */
    URGENT(3);
    
    private final int level;
    
    NotificationPriority(int level) {
        this.level = level;
    }
    
    public int getLevel() {
        return level;
    }
}
