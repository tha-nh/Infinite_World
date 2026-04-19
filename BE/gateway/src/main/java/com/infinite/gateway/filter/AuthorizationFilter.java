package com.infinite.gateway.filter;

import com.infinite.gateway.filter.auth.AuthnFilter;
import com.infinite.gateway.model.AuthorizationConfig;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import lombok.extern.slf4j.Slf4j;
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
            if (auths.contains("public")) {
                return chain.filter(exchange);
            }
            if (auths.contains("authn")) {
                if (!authnFilter.apply(exchange)) {
                    return authnFilter.writeUnauthorizedResponse(exchange);
                }
            }
            return chain.filter(exchange);
        };
    }
}
