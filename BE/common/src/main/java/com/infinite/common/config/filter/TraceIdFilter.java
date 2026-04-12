package com.infinite.common.config.filter;

import com.infinite.common.util.Constant;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceIdFilter extends OncePerRequestFilter {
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String PARENT_SPAN_ID_HEADER = "X-Parent-Span-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String traceId = request.getHeader(TRACE_ID_HEADER);
            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString().replace("-", "");
            }
            String parentSpanId = request.getHeader(PARENT_SPAN_ID_HEADER);
            String spanId = UUID.randomUUID().toString().substring(0, 16);
            MDC.put("traceId", traceId);
            MDC.put("spanId", spanId);
            MDC.put("parentSpanId", parentSpanId == null ? "" : parentSpanId);
            MDC.put("service", Constant.SERVICE_NAME);
            response.setHeader(TRACE_ID_HEADER, traceId);
            response.setHeader(PARENT_SPAN_ID_HEADER, spanId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}