package com.infinite.user.dto.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    
    // Optional fields for login alert
    private String ipAddress;
    private String device;
}
