package com.infinite.notification.contract.enumtype;

/**
 * Action types for notification interactions
 */
public enum NotificationActionType {
    /**
     * No action required
     */
    NONE,
    
    /**
     * Open a specific screen in the app
     */
    OPEN_SCREEN,
    
    /**
     * Open a URL in browser
     */
    OPEN_URL,
    
    /**
     * Open app via deeplink
     */
    OPEN_DEEPLINK,
    
    /**
     * Claim a reward
     */
    CLAIM_REWARD,
    
    /**
     * Navigate to a specific feature
     */
    NAVIGATE
}
