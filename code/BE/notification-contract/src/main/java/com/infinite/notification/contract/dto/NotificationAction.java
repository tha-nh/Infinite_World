package com.infinite.notification.contract.dto;

import com.infinite.notification.contract.enumtype.NotificationActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Action to be performed when user interacts with notification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationAction {
    
    /**
     * Type of action
     */
    @Builder.Default
    private NotificationActionType actionType = NotificationActionType.NONE;
    
    /**
     * Screen name to open (when actionType = OPEN_SCREEN)
     */
    private String screen;
    
    /**
     * URL to open (when actionType = OPEN_URL)
     */
    private String url;
    
    /**
     * Deeplink URI (when actionType = OPEN_DEEPLINK)
     */
    private String deeplink;
    
    /**
     * Additional action payload
     */
    private Map<String, Object> payload;
}
