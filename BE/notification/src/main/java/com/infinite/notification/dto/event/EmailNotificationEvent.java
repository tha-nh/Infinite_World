package com.infinite.notification.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Email notification event
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationEvent {
    
    private String eventId;
    private Instant timestamp;
    
    private String to;
    private String subject;
    private String content;
    private String template;
    private Map<String, Object> variables;
    private boolean isHtml;
    
    private String userId;
    private Map<String, Object> metadata;
    
    @Builder.Default
    private int retryCount = 0;
}
