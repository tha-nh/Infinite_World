package com.infinite.gateway.filter.auth;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

@Component
public class AuthnFilter {

    public boolean apply(ServerWebExchange exchange) {

        String auth = exchange.getRequest()
                .getHeaders()
                .getFirst("Authorization");

        if (!StringUtils.hasText(auth) || !auth.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        return true;
    }
}