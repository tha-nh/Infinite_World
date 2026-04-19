package com.infinite.notification.service;

import com.infinite.notification.dto.request.SmsRequest;

public interface SmsService {
    
    void sendSms(SmsRequest request);
    
    void sendOtpSms(String phoneNumber, String otp, String type);
}