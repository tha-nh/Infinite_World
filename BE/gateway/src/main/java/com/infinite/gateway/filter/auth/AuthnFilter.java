package com.infinite.gateway.filter.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.infinite.common.constant.StatusCode;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.common.dto.response.Response;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;
import java.util.Locale;

@Component
public class AuthnFilter {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Value("${SECRET_KEY}")
    private String jwtSecret;

    public boolean apply(ServerWebExchange exchange) {
        String auth = exchange.getRequest()
                .getHeaders()
                .getFirst("Authorization");

        if (!StringUtils.hasText(auth) || !auth.startsWith("Bearer ")) {
            return false;
        }

        String token = auth.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            return false;
        }

        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public Mono<Void> writeUnauthorizedResponse(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(StatusCode.UNAUTHORIZED.getHttpStatusCode());
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(serializeBody(exchange));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private byte[] serializeBody(ServerWebExchange exchange) {
        Locale previousLocale = LocaleContextHolder.getLocale();
        LocaleContextHolder.setLocale(resolveLocale(exchange));

        try {
            ApiResponse<Object> response = ApiResponse.builder()
                    .code(Response.code(StatusCode.UNAUTHORIZED))
                    .message(Response.message("auth.token"))
                    .build();

            return objectMapper.writeValueAsBytes(response);
        } catch (JsonProcessingException ex) {
            return ("""
                    {"code":1004,"message":"%s"}
                    """.formatted(Response.message("auth.token")))
                    .getBytes(StandardCharsets.UTF_8);
        } finally {
            LocaleContextHolder.setLocale(previousLocale);
        }
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private Locale resolveLocale(ServerWebExchange exchange) {
        List<Locale> locales = exchange.getRequest().getHeaders().getAcceptLanguageAsLocales();
        if (locales == null || locales.isEmpty()) {
            return Locale.ENGLISH;
        }
        return locales.getFirst();
    }
}
