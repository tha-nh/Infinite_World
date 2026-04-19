package com.infinite.common.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * User status change event - for lock/unlock/update notifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusChangeEvent {
    
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    private String to;
    private String userId;
    private String username;
    private String action; // LOCKED, UNLOCKED, UPDATED, AUTO_UNLOCKED
    private LocalDateTime lockTime; // null if permanent lock or not applicable
    private String performedBy;
    private Map<String, Object> metadata;
    
    @Builder.Default
    private String locale = "en"; // Default locale
    
    @Builder.Default
    private int retryCount = 0;
}
