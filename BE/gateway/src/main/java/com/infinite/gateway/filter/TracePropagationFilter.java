package com.infinite.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TracePropagationFilter implements GlobalFilter {
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER = "X-Span-Id";
    private static final String FROM_HEADER = "X-From";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        String spanId = request.getHeaders().getFirst(SPAN_ID_HEADER);
        String from = request.getHeaders().getFirst(FROM_HEADER);
        ServerHttpRequest mutated = request.mutate()
                .headers(headers -> {
                    if (StringUtils.hasText(traceId)) {
                        headers.set(TRACE_ID_HEADER, traceId);
                    }
                    if (StringUtils.hasText(spanId)) {
                        headers.set(SPAN_ID_HEADER, spanId);
                    }
                    if (StringUtils.hasText(from)) {
                        headers.set(FROM_HEADER, from);
                    }
                })
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }
}
