package com.infinite.gateway.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "gateway")
public class RouteConfig {

    private List<Route> routes;

    @Data
    public static class Route {
        private String id;
        private String uri;
        private List<String> predicates;
        private int stripPrefix = 0;
        private List<String> filters;
    }
}