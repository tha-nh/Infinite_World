package com.infinite.user.dto.request;

import lombok.Data;

@Data
public class ForgotPasswordSmsRequest {
    private String phoneNumber;
}