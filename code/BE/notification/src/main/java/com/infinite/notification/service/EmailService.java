package com.infinite.notification.service;

import com.infinite.common.dto.event.EmailNotificationEvent;
import com.infinite.notification.dto.request.EmailRequest;

import java.time.LocalDateTime;

public interface EmailService {
    
    /**
     * Unified method to send templated email based on EmailType
     * This is the new standard way to send emails
     */
    void sendTemplatedEmail(EmailNotificationEvent event);
    
    void sendEmail(EmailRequest request);
    
    void sendOtpEmail(String email, String otp, String type);
    
    void sendVerificationEmail(String email, String verificationUrl);
    
    void sendPasswordResetEmail(String email, String otp);
    
    void sendPasswordResetVerificationEmail(String email, String verificationUrl, String defaultPassword);
    
    // New methods for user status changes
    void sendUserLockedEmail(String email, String username, LocalDateTime lockTime, String performedBy);
    
    void sendUserUnlockedEmail(String email, String username, String performedBy);
    
    void sendUserAutoUnlockedEmail(String email, String username);
    
    void sendUserUpdatedEmail(String email, String username, String performedBy);
    
    // New method for account verification
    void sendAccountVerificationEmail(String email, String username, String verificationToken, String userId);
}