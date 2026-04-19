package com.infinite.notification.messaging;

/**
 * Centralized topic/queue names
 * Change these constants to rename topics without touching business logic
 */
public final class Topics {
    
    private Topics() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    // Notification topics
    public static final String EMAIL_NOTIFICATION = "notification.email";
    public static final String SMS_NOTIFICATION = "notification.sms";
    public static final String WEBSOCKET_NOTIFICATION = "notification.websocket";
    
    // User event topics
    public static final String USER_REGISTERED = "user.registered";
    public static final String USER_VERIFIED = "user.verified";
    public static final String USER_PASSWORD_RESET = "user.password.reset";
    public static final String USER_STATUS_CHANGE = "user.status.change";
    public static final String ACCOUNT_VERIFICATION = "account.verification";
}
