package com.infinite.notification.scheduler;

import com.infinite.notification.infrastructure.persistence.repository.NotificationCleanupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRetentionCleanupScheduler {

    private final NotificationCleanupRepository cleanupRepository;

    @Value("${notification.cleanup.enabled:true}")
    private boolean enabled;

    @Value("${notification.retention.inbox-days:180}")
    private long inboxDays;

    @Value("${notification.retention.email-log-days:90}")
    private long emailLogDays;

    @Value("${notification.retention.delivery-days:90}")
    private long deliveryDays;

    @Scheduled(cron = "${notification.cleanup.cron:0 30 2 * * *}")
    public void cleanup() {
        if (!enabled) {
            return;
        }

        Instant now = Instant.now();
        int userNotifications = cleanupRepository.deleteUserNotificationsBefore(now.minus(inboxDays, ChronoUnit.DAYS));
        int emailLogs = cleanupRepository.deleteEmailDeliveryLogsBefore(now.minus(emailLogDays, ChronoUnit.DAYS));
        int batches = cleanupRepository.deleteDeliveryBatchesBefore(now.minus(deliveryDays, ChronoUnit.DAYS));
        int jobs = cleanupRepository.deleteDeliveryJobsBefore(now.minus(deliveryDays, ChronoUnit.DAYS));

        log.info("Notification retention cleanup completed: userNotifications={}, emailLogs={}, batches={}, jobs={}",
                userNotifications, emailLogs, batches, jobs);
    }
}
