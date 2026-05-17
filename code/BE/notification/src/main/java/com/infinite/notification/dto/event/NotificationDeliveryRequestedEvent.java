package com.infinite.notification.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDeliveryRequestedEvent {

    private Long jobId;
    private Long notificationId;
    private Long requestId;
    private String traceId;
}
