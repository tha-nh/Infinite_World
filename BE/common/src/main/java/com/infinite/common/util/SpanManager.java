package com.infinite.common.util;

import com.infinite.common.dto.logging.Span;
import org.slf4j.MDC;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public class SpanManager {
    private static final ThreadLocal<Deque<Span>> currentSpans = ThreadLocal.withInitial(ArrayDeque::new);

    public static Span startSpan(String name) {
        Span parent = currentSpans.get().peekLast();
        Span span = new Span();
        span.setName(name);
        span.setStartTime(System.currentTimeMillis());

        if (parent != null) {
            span.setTraceId(parent.getTraceId());
            span.setParentSpanId(parent.getSpanId());
        } else {
            span.setTraceId(UUID.randomUUID().toString().replace("-", ""));
            span.setParentSpanId("");
        }

        span.setSpanId(UUID.randomUUID().toString().substring(0,16));
        currentSpans.get().addLast(span);

        MDC.put("traceId", span.getTraceId());
        MDC.put("spanId", span.getSpanId());
        MDC.put("parentSpanId", span.getParentSpanId());

        return span;
    }

    public static void endSpan() {
        Span span = currentSpans.get().pollLast();
        if (span != null) {
            span.setEndTime(System.currentTimeMillis());
        }

        Span current = currentSpans.get().peekLast();
        if (current != null) {
            MDC.put("traceId", current.getTraceId());
            MDC.put("spanId", current.getSpanId());
            MDC.put("parentSpanId", current.getParentSpanId());
        } else {
            MDC.clear();
        }
    }
}