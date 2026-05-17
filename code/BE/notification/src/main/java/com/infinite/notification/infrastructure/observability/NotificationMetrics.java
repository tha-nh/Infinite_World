package com.infinite.notification.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationMetrics {

    private final MeterRegistry meterRegistry;

    public void increment(String name) {
        Counter.builder("notification." + name)
                .register(meterRegistry)
                .increment();
    }

    public void increment(String name, String tag, String value) {
        Counter.builder("notification." + name)
                .tag(tag, value)
                .register(meterRegistry)
                .increment();
    }
}
