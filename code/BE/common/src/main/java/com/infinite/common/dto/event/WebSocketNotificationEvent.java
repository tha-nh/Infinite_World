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
 * 
 * @deprecated This is an internal/downstream contract. Services should use
 *             {@link com.infinite.notification.contract.event.NotificationRequestEvent} instead.
 *             This class will be moved to notification module in future versions.
 */
@Deprecated(since = "1.0.0", forRemoval = true)
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
