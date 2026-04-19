package com.infinite.notification.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * WebSocket notification event
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketNotificationEvent {
    
    private String eventId;
    private Instant timestamp;
    
    private String userId;
    private String type;
    private String title;
    private String message;
    private boolean broadcast;
    
    private Map<String, Object> metadata;
    
    @Builder.Default
    private int retryCount = 0;
}
