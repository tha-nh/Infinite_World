package com.infinite.user.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class LoginResponse {
    private String username;
    private String name;
    private String email;
    private String phoneNumber;
    private String imageUrl;
    private String token;
    private Set<String> roles;
}