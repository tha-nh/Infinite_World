package com.infinite.gateway.filter.auth;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

@Component
public class AuthzFilter {

    public boolean apply(ServerWebExchange exchange, List<String> roles) {
        String userRole = exchange.getRequest()
                .getHeaders()
                .getFirst("X-ROLE");

        if (userRole == null) {
            return false;
        }
        return roles.stream()
                .anyMatch(r -> r.equalsIgnoreCase(userRole));
    }
}