package com.infinite.gateway.filter.auth;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AuthzFilter {

    public boolean apply(ServerWebExchange exchange, List<String> requiredRoles) {
        // Get user roles from header set by AuthnFilter
        String userRolesHeader = exchange.getRequest()
                .getHeaders()
                .getFirst("X-USER-ROLES");

        if (userRolesHeader == null || userRolesHeader.isEmpty()) {
            return false;
        }

        // Parse user roles from comma-separated string
        Set<String> userRoles = Arrays.stream(userRolesHeader.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .collect(Collectors.toSet());

        // Check if user has any of the required roles
        return requiredRoles.stream()
                .anyMatch(requiredRole -> userRoles.contains(requiredRole));
    }
}