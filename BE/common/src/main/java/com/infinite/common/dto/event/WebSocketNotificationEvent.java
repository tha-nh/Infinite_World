package com.infinite.common.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket notification event - shared across services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketNotificationEvent {
    
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    private String userId;
    private String type;
    private String title;
    private String message;
    
    @Builder.Default
    private boolean broadcast = false;
    
    private Map<String, Object> metadata;
    
    @Builder.Default
    private int retryCount = 0;
}
