package com.infinite.user.dto.request;

import lombok.Data;

@Data
public class VerifyRegistrationRequest {
    private String identifier; // email or phoneNumber
    private String otp;
    private String verificationMethod; // "email" or "sms"
}