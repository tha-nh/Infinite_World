package com.infinite.notification.service;

import com.infinite.notification.dto.request.EmailRequest;

public interface EmailService {
    
    void sendEmail(EmailRequest request);
    
    void sendOtpEmail(String email, String otp, String type);
    
    void sendVerificationEmail(String email, String verificationUrl);
    
    void sendPasswordResetEmail(String email, String otp);
}