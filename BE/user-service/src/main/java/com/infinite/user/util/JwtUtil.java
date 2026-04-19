package com.infinite.user.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class JwtUtil {

    private @Value("${secret.key}") String jwtSecret;
    private @Value("${jwt.expiration.ms}") long jwtExpirationMs;
    
    private static final long RESET_TOKEN_EXPIRATION_MS = 3600000; // 1 hour

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateToken(String username, Set<String> roles) {
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateToken() {
        return Jwts.builder()
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateResetToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("type", "reset")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + RESET_TOKEN_EXPIRATION_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    @SuppressWarnings("unchecked")
    public Set<String> extractRoles(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        List<String> rolesList = claims.get("roles", List.class);
        return rolesList != null ? Set.copyOf(rolesList) : Set.of();
    }

    public boolean validate(String token, UserDetails user) {
        return extractUsername(token).equals(user.getUsername());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public boolean validateResetToken(String token, String email) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            String tokenEmail = claims.getSubject();
            String tokenType = claims.get("type", String.class);
            Date expiration = claims.getExpiration();
            
            return tokenEmail.equals(email) 
                    && "reset".equals(tokenType)
                    && expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid reset token: {}", e.getMessage());
            return false;
        }
    }
}