package com.infinite.notification.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * SMS notification event
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsNotificationEvent {
    
    private String eventId;
    private Instant timestamp;
    
    private String phoneNumber;
    private String message;
    private String template;
    private Map<String, Object> variables;
    
    private String userId;
    private Map<String, Object> metadata;
    
    @Builder.Default
    private int retryCount = 0;
}
