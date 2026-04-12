package com.infinite.gateway.config;

import com.infinite.gateway.filter.AuthorizationFilter;
import com.infinite.gateway.model.AuthorizationConfig;
import com.infinite.gateway.model.RouteConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.BooleanSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class GatewayRouteLocatorConfig {

    private final RouteConfig routeConfig;
    private final AuthorizationFilter authorizationFilter;

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        RouteLocatorBuilder.Builder routes = builder.routes();
        for (RouteConfig.Route r : routeConfig.getRoutes()) {
            routes.route(r.getId(), rSpec -> {
                BooleanSpec spec = rSpec.path("/**");
                String path = null;
                String method = null;
                if (r.getPredicates() != null) {
                    for (String p : r.getPredicates()) {
                        if (p.startsWith("Path=")) {
                            path = p.substring("Path=".length()).trim();
                        }
                        if (p.startsWith("Method=")) {
                            method = p.substring("Method=".length()).trim();
                        }
                    }
                }
                if (path != null) {
                    spec = rSpec.path(path);
                }
                if (method != null) {
                    String[] methods = Arrays.stream(method.split(","))
                            .map(String::trim)
                            .toArray(String[]::new);
                    spec = spec.and().method(methods);
                }
                return spec
                        .filters(f -> {
                            if (r.getStripPrefix() > 0) {
                                f.stripPrefix(r.getStripPrefix());
                            }
                            if (r.getFilters() != null && !r.getFilters().isEmpty()) {
                                AuthorizationConfig config = new AuthorizationConfig();
                                for (String filterStr : r.getFilters()) {
                                    if (filterStr.startsWith("Authorization=")) {
                                        String value = filterStr.substring("Authorization=".length()).trim();
                                        config.setAuth(
                                                Arrays.stream(value.split(","))
                                                        .map(String::trim)
                                                        .toList()
                                        );
                                    }
                                    if (filterStr.startsWith("Role=")) {
                                        String value = filterStr.substring("Role=".length()).trim();
                                        config.setRoles(
                                                Arrays.stream(value.split(","))
                                                        .map(String::trim)
                                                        .toList()
                                        );
                                    }
                                }
                                f.filter(authorizationFilter.apply(config));
                            }
                            return f;
                        })
                        .uri(r.getUri());
            });
        }
        return routes.build();
    }
}