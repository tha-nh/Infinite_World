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

/**
 * Filter to set default event="debug" for all logs in request context
 * This runs after TraceIdFilter and before AccessFilter
 */
@Component
@Order(0) // Before TraceIdFilter (1) and AccessFilter (2)
public class DebugEventFilter extends OncePerRequestFilter {
    
    private static final String EVENT_KEY = "event";
    private static final String DEBUG_EVENT = "debug";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            // Set default event="debug" for all logs in this request
            MDC.put(EVENT_KEY, DEBUG_EVENT);
            
            filterChain.doFilter(request, response);
            
        } finally {
            // Don't remove event here, let AccessFilter handle it
        }
    }
}