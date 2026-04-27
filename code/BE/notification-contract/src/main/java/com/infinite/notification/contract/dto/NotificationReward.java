package com.infinite.notification.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Reward attached to notification (if claimable)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationReward {
    
    /**
     * Reward type (e.g., "COIN", "ITEM", "VOUCHER")
     */
    private String rewardType;
    
    /**
     * Reward amount or quantity
     */
    private Long amount;
    
    /**
     * Item ID if reward is an item
     */
    private String itemId;
    
    /**
     * Whether reward can be claimed
     */
    @Builder.Default
    private Boolean claimable = false;
    
    /**
     * Additional reward metadata
     */
    private Map<String, Object> metadata;
}
