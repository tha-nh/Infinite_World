package com.infinite.user.dto.request;

import lombok.Data;

@Data
public class VerifyEmailRequest {
    private String token;
    private String action;
    private String lang;
}