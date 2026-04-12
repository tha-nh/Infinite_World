package com.infinite.gateway.model;

import lombok.Data;

import java.util.List;

@Data
public class AuthorizationConfig {
    private List<String> auth;
    private List<String> roles;
}