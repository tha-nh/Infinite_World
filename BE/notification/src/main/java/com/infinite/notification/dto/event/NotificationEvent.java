package com.infinite.notification.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Base notification event
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    
    private String eventId;
    private String eventType;
    private Instant timestamp;
    private String userId;
    private Map<String, Object> metadata;
    
    @Builder.Default
    private int retryCount = 0;
}
