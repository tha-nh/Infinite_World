package com.infinite.notification.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDlqEvent {

    private String sourceTopic;
    private String sourceKey;
    private String failureStage;
    private String payload;
    private String errorMessage;
    private Integer retryCount;
    private Instant occurredAt;
}
