package com.infinite.user.dto.request;

import lombok.Data;

@Data
public class ForgotPasswordSmsOtpRequest {
    private String phoneNumber;
    private String otp;
    private String newPassword;
}