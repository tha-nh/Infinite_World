package com.infinite.user.dto.request;

import lombok.Data;

@Data
public class RegistrationRequest {
    private String username;
    private String password;
    private String name;
    private String email;
    private String phoneNumber;
    private String verificationMethod; // "email" or "sms"
    private String nguoithuchien;
}