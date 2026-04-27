package com.infinite.common.constant;

/**
 * Email template types for standardized email notifications
 */
public enum EmailType {
    // OTP emails
    LOGIN_OTP,
    FORGOT_PASSWORD_OTP,
    REGISTRATION_OTP,
    
    // Verification emails
    REGISTRATION_VERIFICATION,
    PASSWORD_RESET_VERIFICATION,
    
    // User status change emails
    USER_LOCKED,
    USER_UNLOCKED,
    USER_AUTO_UNLOCKED,
    USER_UPDATED,
    
    // Login alert
    LOGIN_ALERT
}
