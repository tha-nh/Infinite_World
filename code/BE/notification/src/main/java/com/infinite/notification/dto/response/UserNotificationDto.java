package com.infinite.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationDto {
    private Long id;
    private String title;
    private String body;
    private String type;
    private Short priority;
    private String imageUrl;
    private Map<String, Object> actionPayload;
    private Map<String, Object> rewardPayload;
    private String status;
    private Boolean isClaimed;
    private Instant readAt;
    private Instant claimedAt;
    private Instant expireAt;
    private Instant createdAt;
}
