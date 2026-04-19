package com.infinite.common.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Email notification event - shared across services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationEvent {
    
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    private String to;
    private String subject;
    private String content;
    private String template;
    private Map<String, Object> variables;
    
    @Builder.Default
    private boolean isHtml = true;
    
    private String userId;
    private Map<String, Object> metadata;
    
    @Builder.Default
    private String locale = "en"; // Default locale
    
    @Builder.Default
    private int retryCount = 0;
}
