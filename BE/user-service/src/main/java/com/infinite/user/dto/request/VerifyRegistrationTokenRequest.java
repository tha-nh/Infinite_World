package com.infinite.user.dto.request;

import lombok.Data;

@Data
public class VerifyRegistrationTokenRequest {
    private String token;
    private String action; // "approve" or "reject"
    private String lang;
}