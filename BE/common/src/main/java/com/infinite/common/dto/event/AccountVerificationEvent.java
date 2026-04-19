package com.infinite.common.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Account verification event - for registration confirmation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountVerificationEvent {
    
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    private String to;
    private String userId;
    private String username;
    private String verificationToken;
    private Map<String, Object> metadata;
    
    @Builder.Default
    private String locale = "en"; // Default locale
    
    @Builder.Default
    private int retryCount = 0;
}
