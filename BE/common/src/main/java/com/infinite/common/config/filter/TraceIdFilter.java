package com.infinite.common.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class TraceIdFilter extends OncePerRequestFilter {
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String PARENT_SPAN_ID_HEADER = "X-Span-Id";
    public static final String FROM_HEADER = "X-From";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String traceId = request.getHeader(TRACE_ID_HEADER);
            String spanId = UUID.randomUUID().toString().substring(0, 16);
            String parentSpanId = request.getHeader(PARENT_SPAN_ID_HEADER);
            String from = request.getHeader(FROM_HEADER);
            MDC.put("traceId", traceId);
            MDC.put("spanId", spanId);
            MDC.put("parentSpanId", parentSpanId);
            MDC.put("from", from);
            response.setHeader(TRACE_ID_HEADER, traceId);
            response.setHeader(PARENT_SPAN_ID_HEADER, spanId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
            MDC.remove("spanId");
            MDC.remove("parentSpanId");
            MDC.remove("from");
        }
    }
}
