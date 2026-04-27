package com.infinite.common.service;

import java.util.concurrent.TimeUnit;

public interface OtpService {
    
    String generateOtp();
    
    void storeOtp(String key, String otp, long timeout, TimeUnit timeUnit);
    
    String getOtp(String key);
    
    boolean verifyOtp(String key, String inputOtp);
    
    void deleteOtp(String key);
    
    String generateForgotPasswordOtpKey(String email);
    
    String generateRegistrationOtpKey(String email);
    
    String generateLoginOtpKey(String identifier);
    
    String generatePhoneVerificationOtpKey(String phoneNumber);
    
    String generateEmailVerificationOtpKey(String email);
    
    void storeOtpWithDefaultExpiration(String key, String otp);
}
