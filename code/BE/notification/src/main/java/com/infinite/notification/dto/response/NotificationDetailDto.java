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
public class NotificationDetailDto {
    private Long id;
    private Long requestId;
    private String code;
    private String title;
    private String body;
    private String type;
    private Short priority;
    private String imageUrl;
    private Map<String, Object> actionPayload;
    private Map<String, Object> rewardPayload;
    private Map<String, Object> channelPayload;
    private Instant startAt;
    private Instant expireAt;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
