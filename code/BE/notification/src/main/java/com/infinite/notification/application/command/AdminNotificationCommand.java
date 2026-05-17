package com.infinite.notification.application.command;

import com.infinite.common.exception.BadRequestException;
import com.infinite.common.exception.NotFoundException;
import com.infinite.common.util.I18n;
import com.infinite.notification.infrastructure.persistence.entity.NotificationDeliveryJob;
import com.infinite.notification.infrastructure.persistence.entity.NotificationTemplate;
import com.infinite.notification.infrastructure.persistence.repository.NotificationDeliveryJobRepository;
import com.infinite.notification.infrastructure.persistence.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationCommand {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationDeliveryJobRepository deliveryJobRepository;

    @Transactional
    public void cancel(Long notificationId) {
        NotificationTemplate template = templateRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException(I18n.msg("notification.not.found")));

        if ("CANCELED".equals(template.getStatus())) {
            return;
        }
        if ("DELIVERED".equals(template.getStatus())) {
            throw new BadRequestException(I18n.msg("notification.admin.cancel.delivered"));
        }

        template.setStatus("CANCELED");
        templateRepository.save(template);

        List<NotificationDeliveryJob> jobs = deliveryJobRepository.findByNotificationId(notificationId);
        Instant now = Instant.now();
        for (NotificationDeliveryJob job : jobs) {
            if (!"COMPLETED".equals(job.getStatus())) {
                job.setStatus("CANCELED");
                job.setFinishedAt(now);
            }
        }
        deliveryJobRepository.saveAll(jobs);
        log.info("Cancelled notification: id={}, affectedJobs={}", notificationId, jobs.size());
    }

    @Transactional
    public void retry(Long notificationId) {
        NotificationTemplate template = templateRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException(I18n.msg("notification.not.found")));

        if ("DELIVERED".equals(template.getStatus())) {
            throw new BadRequestException(I18n.msg("notification.admin.retry.delivered"));
        }

        template.setStatus("PENDING");
        templateRepository.save(template);

        List<NotificationDeliveryJob> jobs = deliveryJobRepository.findByNotificationId(notificationId);
        Instant now = Instant.now();
        if (jobs.isEmpty()) {
            NotificationDeliveryJob job = NotificationDeliveryJob.builder()
                    .notificationId(notificationId)
                    .jobType("INBOX_FANOUT")
                    .status("PENDING")
                    .totalTarget(0L)
                    .processedTarget(0L)
                    .successTarget(0L)
                    .failedTarget(0L)
                    .retryCount(1)
                    .scheduledAt(now)
                    .build();
            deliveryJobRepository.save(job);
            log.info("Created retry job for notification: id={}", notificationId);
            return;
        }

        for (NotificationDeliveryJob job : jobs) {
            job.setStatus("PENDING");
            job.setRetryCount(job.getRetryCount() == null ? 1 : job.getRetryCount() + 1);
            job.setLastError(null);
            job.setScheduledAt(now);
            job.setStartedAt(null);
            job.setFinishedAt(null);
        }
        deliveryJobRepository.saveAll(jobs);
        log.info("Retried notification: id={}, affectedJobs={}", notificationId, jobs.size());
    }
}
