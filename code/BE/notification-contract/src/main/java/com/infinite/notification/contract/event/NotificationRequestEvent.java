package com.infinite.notification.contract.event;

import com.infinite.notification.contract.dto.NotificationAction;
import com.infinite.notification.contract.dto.NotificationContent;
import com.infinite.notification.contract.dto.NotificationReward;
import com.infinite.notification.contract.dto.NotificationTarget;
import com.infinite.notification.contract.enumtype.NotificationChannel;
import com.infinite.notification.contract.metadata.BaseNotificationEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Main notification request event
 * This is the primary contract for services to request notifications
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationRequestEvent extends BaseNotificationEvent {
    
    /**
     * Delivery channels for this notification
     */
    private Set<NotificationChannel> channels;
    
    /**
     * Target audience
     */
    private NotificationTarget target;
    
    /**
     * Notification content
     */
    private NotificationContent content;
    
    /**
     * Action to perform on interaction
     */
    private NotificationAction action;
    
    /**
     * Reward attached to notification (optional)
     */
    private NotificationReward reward;
    
    /**
     * When notification should start being visible
     */
    private Instant startAt;
    
    /**
     * When notification expires
     */
    private Instant expireAt;
    
    /**
     * Additional business metadata
     */
    private Map<String, Object> metadata;
}
