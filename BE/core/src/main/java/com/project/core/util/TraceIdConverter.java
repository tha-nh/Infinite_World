package com.project.core.util;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.MDC;

public class TraceIdConverter extends ClassicConverter {
    @Override
    public String convert(ILoggingEvent event) {
        String trace = MDC.get("traceId");
        String span = MDC.get("spanId");
        if ((trace == null || trace.isEmpty()) && (span == null || span.isEmpty())) {
            return ""; // bỏ hoàn toàn nếu rỗng
        }
        return "[" + trace + "," + span + "]";
    }
}