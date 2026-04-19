package com.infinite.notification.service.impl;

import com.infinite.common.constant.OtpConstant;
import com.infinite.common.constant.SmsTemplate;
import com.infinite.common.util.MessageUtils;
import com.infinite.notification.dto.request.SmsRequest;
import com.infinite.notification.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {
    
    @Value("${sms.provider.url}")
    private String smsProviderUrl;
    
    @Value("${sms.provider.account-sid}")
    private String accountSid;
    
    @Value("${sms.provider.auth-token}")
    private String authToken;
    
    @Value("${sms.provider.from-number}")
    private String fromNumber;
    
    @Value("${sms.mock.mode}")
    private boolean mockMode;
    
    private final RestTemplate restTemplate;
    
    @Override
    public void sendSms(SmsRequest request) {
        try {
            if (mockMode || accountSid.isEmpty() || authToken.isEmpty()) {
                log.info("SMS Mock - To: {}, Message: {}", request.getPhoneNumber(), request.getMessage());
                return;
            }
            
            sendViaTwilio(request);
            
        } catch (Exception e) {
            log.error("Failed to send SMS to: {}", request.getPhoneNumber(), e);
            throw new RuntimeException("Failed to send SMS", e);
        }
    }
    
    @Override
    public void sendOtpSms(String phoneNumber, String otp, String type) {
        String message = getOtpMessage(otp, type);
        SmsRequest request = SmsRequest.builder()
                .phoneNumber(phoneNumber)
                .message(message)
                .build();
        sendSms(request);
    }
    
    private void sendViaTwilio(SmsRequest request) {
        String url = smsProviderUrl + "/" + accountSid + "/Messages.json";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(accountSid, authToken);
        
        Map<String, String> body = Map.of(
            "From", fromNumber,
            "To", request.getPhoneNumber(),
            "Body", request.getMessage()
        );
        
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
        restTemplate.postForObject(url, entity, String.class);
        
        log.info("SMS sent successfully to: {}", request.getPhoneNumber());
    }
    
    private String getOtpMessage(String otp, String type) {
        String purposeKey = switch (type) {
            case "forgot_password" -> SmsTemplate.OTP_PURPOSE_FORGOT_PASSWORD;
            case "registration" -> SmsTemplate.OTP_PURPOSE_REGISTRATION;
            case "login" -> SmsTemplate.OTP_PURPOSE_LOGIN;
            default -> SmsTemplate.OTP_PURPOSE_VERIFICATION;
        };
        
        long expirationMinutes = switch (type) {
            case "forgot_password" -> OtpConstant.DEFAULT_OTP_EXPIRATION_MINUTES;
            case "registration" -> OtpConstant.REGISTRATION_OTP_EXPIRATION_MINUTES;
            case "login" -> OtpConstant.LOGIN_OTP_EXPIRATION_MINUTES;
            default -> OtpConstant.DEFAULT_OTP_EXPIRATION_MINUTES;
        };
        
        String purpose = MessageUtils.getMessage(purposeKey);
        return MessageUtils.getMessage(SmsTemplate.OTP_CONTENT, 
                purpose, otp, String.valueOf(expirationMinutes));
    }
}