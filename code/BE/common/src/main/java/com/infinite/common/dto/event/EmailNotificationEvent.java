package com.infinite.common.dto.event;

import com.infinite.common.constant.EmailType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Unified email notification event - the single event for all email types
 * Replaces AccountVerificationEvent and UserStatusChangeEvent
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
public class EmailNotificationEvent {
    
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    // Recipient
    private String to;
    
    // Email type (required) - determines which template to use
    private EmailType emailType;
    
    // Template variables (required) - data needed to render the email
    private Map<String, Object> variables;
    
    // User context
    private String userId;
    
    // Locale for i18n
    @Builder.Default
    private String locale = "en";
    
    // Technical metadata (optional) - for tracking, debugging
    private Map<String, Object> metadata;
    
    @Builder.Default
    private int retryCount = 0;
    
    // Deprecated fields - kept for backward compatibility, will be removed
    @Deprecated
    private String subject;
    
    @Deprecated
    private String content;
    
    @Deprecated
    private String template;
    
    @Deprecated
    @Builder.Default
    private boolean isHtml = true;
}
