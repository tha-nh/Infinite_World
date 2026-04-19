package com.infinite.common.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * SMS notification event - shared across services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsNotificationEvent {
    
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    private String phoneNumber;
    private String message;
    private String template;
    private Map<String, Object> variables;
    
    private String userId;
    private Map<String, Object> metadata;
    
    @Builder.Default
    private int retryCount = 0;
}
