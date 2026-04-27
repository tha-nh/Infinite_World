package com.infinite.notification.contract.enumtype;

/**
 * Target audience type for notifications
 */
public enum NotificationTargetType {
    /**
     * Target specific user IDs
     */
    USER_IDS,
    
    /**
     * Target users by role
     */
    ROLE,
    
    /**
     * Target all users
     */
    ALL,
    
    /**
     * Target user segment (e.g., VIP, active players)
     */
    SEGMENT,
    
    /**
     * Target by custom query/filter
     */
    QUERY
}
