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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@RequiredArgsConstructor
public class GatewayRouteLocatorConfig {

    private final RouteConfig routeConfig;
    private final AuthorizationFilter authorizationFilter;

    /**
     * Parse path patterns from various formats:
     * - Single: /v1/api/auth/**
     * - Array: ["/v1/api/auth/**", "/v1/api/user/**"]
     * - Comma-separated: /v1/api/auth/**,/v1/api/user/**
     */
    private List<String> parsePaths(String pathStr) {
        List<String> paths = new ArrayList<>();

        if (pathStr == null || pathStr.isEmpty()) {
            return paths;
        }

        pathStr = pathStr.trim();

        // Handle array format: ["/path1", "/path2"]
        if (pathStr.startsWith("[") && pathStr.endsWith("]")) {
            String content = pathStr.substring(1, pathStr.length() - 1);
            Pattern pattern = Pattern.compile("\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String path = matcher.group(1).trim();
                if (!path.isEmpty()) {
                    paths.add(path);
                }
            }
        }
        // Handle single path or comma-separated
        else {
            String[] parts = pathStr.split(",");
            for (String part : parts) {
                String path = part.trim();
                if (!path.isEmpty()) {
                    paths.add(path);
                }
            }
        }

        return paths;
    }

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
                    // Parse multiple paths
                    List<String> paths = parsePaths(path);
                    if (!paths.isEmpty()) {
                        if (paths.size() == 1) {
                            spec = rSpec.path(paths.get(0));
                        } else {
                            // Multiple paths: build OR predicate
                            spec = rSpec.predicate(exchange -> {
                                String requestPath = exchange.getRequest().getURI().getPath();
                                for (String p : paths) {
                                    if (pathMatches(requestPath, p)) {
                                        return true;
                                    }
                                }
                                return false;
                            });
                        }
                    }
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
                                                        .toList());
                                    }
                                    if (filterStr.startsWith("Role=")) {
                                        String value = filterStr.substring("Role=".length()).trim();
                                        config.setRoles(
                                                Arrays.stream(value.split(","))
                                                        .map(String::trim)
                                                        .toList());
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

    private boolean pathMatches(String requestPath, String pattern) {
        // Simple pattern matching for ** and * wildcards
        String regex = pattern
                .replace(".", "\\.")
                .replace("**", ".*")
                .replace("*", "[^/]*");
        return requestPath.matches(regex);
    }
}