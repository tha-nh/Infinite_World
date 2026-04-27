package com.infinite.notification.contract.dto;

import com.infinite.notification.contract.enumtype.NotificationTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Defines the target audience for a notification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTarget {
    
    /**
     * Type of target
     */
    private NotificationTargetType type;
    
    /**
     * List of user IDs (when type = USER_IDS)
     */
    private List<Long> userIds;
    
    /**
     * List of roles (when type = ROLE)
     */
    private List<String> roles;
    
    /**
     * Segment identifier (when type = SEGMENT)
     */
    private String segment;
    
    /**
     * Custom query parameters (when type = QUERY)
     */
    private Map<String, Object> queryParams;
}
