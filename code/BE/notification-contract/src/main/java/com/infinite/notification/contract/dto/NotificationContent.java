package com.infinite.notification.contract.dto;

import com.infinite.notification.contract.enumtype.NotificationPriority;
import com.infinite.notification.contract.enumtype.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Notification content details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationContent {
    
    /**
     * Notification type
     */
    @NotNull(message = "content.type is required")
    private NotificationType type;
    
    /**
     * Priority level
     */
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;
    
    /**
     * Notification title
     */
    @NotBlank(message = "content.title is required")
    private String title;
    
    /**
     * Notification body/message
     */
    @NotBlank(message = "content.body is required")
    private String body;
    
    /**
     * Optional image URL
     */
    private String imageUrl;
    
    /**
     * Locale for content (e.g., "en", "vi")
     */
    private String locale;
    
    /**
     * Template code if using predefined templates
     */
    private String templateCode;
    
    /**
     * Template variables for dynamic content
     */
    private Map<String, Object> templateVars;
}
