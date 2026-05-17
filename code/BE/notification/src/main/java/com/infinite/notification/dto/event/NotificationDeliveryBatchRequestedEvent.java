package com.infinite.notification.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDeliveryBatchRequestedEvent {

    private Long jobId;
    private Long batchId;
    private Long notificationId;
}
