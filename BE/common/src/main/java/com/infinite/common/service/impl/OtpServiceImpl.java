package com.infinite.common.service.impl;

import com.infinite.common.constant.OtpConstant;
import com.infinite.common.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Override
    public String generateOtp() {
        int otp = OtpConstant.OTP_MIN_VALUE + secureRandom.nextInt(OtpConstant.OTP_MAX_VALUE - OtpConstant.OTP_MIN_VALUE + 1);
        return String.valueOf(otp);
    }
    
    @Override
    public void storeOtp(String key, String otp, long timeout, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, otp, timeout, timeUnit);
    }
    
    @Override
    public String getOtp(String key) {
        return redisTemplate.opsForValue().get(key);
    }
    
    @Override
    public boolean verifyOtp(String key, String inputOtp) {
        if (inputOtp == null || inputOtp.trim().isEmpty()) {
            return false;
        }
        String storedOtp = getOtp(key);
        return storedOtp != null && storedOtp.equals(inputOtp.trim());
    }
    
    @Override
    public void deleteOtp(String key) {
        redisTemplate.delete(key);
    }
    
    @Override
    public String generateForgotPasswordOtpKey(String email) {
        return OtpConstant.FORGOT_PASSWORD_OTP_PREFIX + email;
    }
    
    @Override
    public String generateRegistrationOtpKey(String email) {
        return OtpConstant.REGISTRATION_OTP_PREFIX + email;
    }
    
    @Override
    public String generateLoginOtpKey(String identifier) {
        return OtpConstant.LOGIN_OTP_PREFIX + identifier;
    }
    
    @Override
    public String generatePhoneVerificationOtpKey(String phoneNumber) {
        return OtpConstant.PHONE_VERIFICATION_OTP_PREFIX + phoneNumber;
    }
    
    @Override
    public String generateEmailVerificationOtpKey(String email) {
        return OtpConstant.EMAIL_VERIFICATION_OTP_PREFIX + email;
    }
    
    @Override
    public void storeOtpWithDefaultExpiration(String key, String otp) {
        storeOtp(key, otp, OtpConstant.DEFAULT_OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);
    }
}