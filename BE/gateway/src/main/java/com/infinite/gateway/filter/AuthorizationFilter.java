package com.infinite.gateway.filter;

import com.infinite.gateway.filter.auth.AuthnFilter;
import com.infinite.gateway.filter.auth.AuthzFilter;
import com.infinite.gateway.model.AuthorizationConfig;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class AuthorizationFilter
        extends AbstractGatewayFilterFactory<AuthorizationConfig> {

    private final AuthnFilter authnFilter;
    private final AuthzFilter authzFilter;

    public AuthorizationFilter(
            AuthnFilter authnFilter,
            AuthzFilter authzFilter
    ) {
        super(AuthorizationConfig.class);
        this.authnFilter = authnFilter;
        this.authzFilter = authzFilter;
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
                if (!authnFilter.apply(exchange)) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
            }
            if (auths.contains("authz")) {
                if (!authzFilter.apply(exchange, roles)) {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }
            }
            return chain.filter(exchange);
        };
    }
}