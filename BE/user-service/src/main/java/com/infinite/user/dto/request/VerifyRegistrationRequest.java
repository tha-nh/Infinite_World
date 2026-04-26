package com.infinite.user.dto.request;

import lombok.Data;

@Data
public class VerifyRegistrationRequest {
    private String identifier; // email only - phone number not supported
    private String otp;
    private String verificationMethod; // "email" only - SMS not supported
}