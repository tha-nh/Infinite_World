package com.infinite.gateway.filter;

import com.infinite.gateway.filter.auth.AuthnFilter;
import com.infinite.gateway.model.AuthorizationConfig;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class AuthorizationFilter
        extends AbstractGatewayFilterFactory<AuthorizationConfig> {

    private final AuthnFilter authnFilter;

    public AuthorizationFilter(AuthnFilter authnFilter) {
        super(AuthorizationConfig.class);
        this.authnFilter = authnFilter;
    }

    @Override
    public AuthorizationConfig newConfig() {
        return new AuthorizationConfig();
    }

    @Override
    public GatewayFilter apply(AuthorizationConfig config) {
        return (exchange, chain) -> {
            List<String> auths = config.getAuth();
            List<String> roles = config.getRoles();
            
            if (auths.contains("public")) {
                return chain.filter(exchange);
            }
            
            if (auths.contains("authn")) {
                // Nếu có roles được chỉ định, kiểm tra roles
                // Nếu không có roles, chỉ kiểm tra token
                if (roles != null && !roles.isEmpty()) {
                    if (!authnFilter.apply(exchange, roles)) {
                        return authnFilter.writeUnauthorizedResponse(exchange);
                    }
                } else {
                    if (!authnFilter.apply(exchange)) {
                        return authnFilter.writeUnauthorizedResponse(exchange);
                    }
                }
            }
            
            if (auths.contains("authn")) {
                ServerHttpRequest request = exchange.getRequest().mutate()
                        .headers(headers -> {
                            headers.remove("X-USER-ID");
                            headers.remove("X-USER-NAME");
                            headers.remove("X-USER-ROLES");
                        })
                        .header("X-USER-ID", String.valueOf(exchange.getAttribute("auth.userId")))
                        .header("X-USER-NAME", String.valueOf(exchange.getAttribute("auth.username")))
                        .header("X-USER-ROLES", String.valueOf(exchange.getAttribute("auth.roles")))
                        .build();
                return chain.filter(exchange.mutate().request(request).build());
            }

            return chain.filter(exchange);
        };
    }
}
