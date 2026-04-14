package com.infinite.gateway.filter.auth;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Component
public class AuthnFilter {

    @Value("${SECRET_KEY}")
    private String jwtSecret;

    public boolean apply(ServerWebExchange exchange) {
        String auth = exchange.getRequest()
                .getHeaders()
                .getFirst("Authorization");

        if (!StringUtils.hasText(auth) || !auth.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        String token = auth.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
