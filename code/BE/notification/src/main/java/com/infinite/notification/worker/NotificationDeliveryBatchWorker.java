package com.infinite.notification.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infinite.notification.dto.event.NotificationDlqEvent;
import com.infinite.notification.application.command.EnqueueEmailDeliveryUseCase;
import com.infinite.notification.dto.event.NotificationDeliveryBatchRequestedEvent;
import com.infinite.notification.infrastructure.messaging.RealtimeDispatcher;
import com.infinite.notification.infrastructure.observability.NotificationMetrics;
import com.infinite.notification.infrastructure.persistence.entity.NotificationDeliveryBatch;
import com.infinite.notification.infrastructure.persistence.entity.NotificationDeliveryJob;
import com.infinite.notification.infrastructure.persistence.entity.NotificationTargetRule;
import com.infinite.notification.infrastructure.persistence.entity.NotificationTemplate;
import com.infinite.notification.infrastructure.persistence.repository.*;
import com.infinite.notification.infrastructure.redis.UnreadCountCacheService;
import com.infinite.notification.messaging.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryBatchWorker {

    private final NotificationDeliveryBatchRepository batchRepository;
    private final NotificationDeliveryJobRepository jobRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationTargetRuleRepository targetRuleRepository;
    private final UserNotificationBulkRepository userNotificationBulkRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final RealtimeDispatcher realtimeDispatcher;
    private final EnqueueEmailDeliveryUseCase enqueueEmailDeliveryUseCase;
    private final UnreadCountCacheService unreadCountCacheService;
    private final MessagePublisher messagePublisher;
    private final ObjectMapper objectMapper;
    private final NotificationMetrics metrics;

    @Value("${notification.fanout.max-retry:3}")
    private int maxRetry;

    @Value("${messaging.topics.notification-dlq:notification.dlq}")
    private String dlqTopic;

    @KafkaListener(
            topics = "${messaging.topics.notification-delivery-batch-requested:notification.delivery.batch.requested}",
            groupId = "${spring.kafka.consumer.group-id:notification-service}",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consume(@Payload String payload, Acknowledgment acknowledgment) {
        NotificationDeliveryBatchRequestedEvent event = null;
        try {
            event = objectMapper.readValue(payload, NotificationDeliveryBatchRequestedEvent.class);
            process(event);
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Delivery batch worker failed: payload={}", payload, ex);
            boolean terminalFailure = event == null;
            if (event != null) {
                terminalFailure = markFailed(event.getBatchId(), ex);
            }
            if (terminalFailure) {
                publishDlq(payload, event != null ? String.valueOf(event.getBatchId()) : null,
                        "delivery-batch", ex, event != null ? retryCount(event.getBatchId()) : null);
                acknowledgment.acknowledge();
            }
        }
    }

    @Transactional
    public void process(NotificationDeliveryBatchRequestedEvent event) {
        NotificationDeliveryBatch batch = batchRepository.findById(event.getBatchId()).orElseThrow();
        if ("COMPLETED".equals(batch.getStatus())) {
            return;
        }

        NotificationDeliveryJob job = jobRepository.findById(batch.getJobId()).orElseThrow();
        NotificationTemplate template = templateRepository.findById(job.getNotificationId()).orElseThrow();
        List<NotificationTargetRule> rules = targetRuleRepository.findByNotificationId(job.getNotificationId());

        List<Long> allUserIds = resolveUserIds(rules);
        Map<Long, String> emailByUserId = resolveEmailByUserId(rules);
        int start = batch.getCursorValue() == null ? 0 : batch.getCursorValue().intValue();
        int end = Math.min(start + batch.getExpectedCount(), allUserIds.size());
        List<Long> batchUserIds = start >= end ? List.of() : allUserIds.subList(start, end);

        batch.setStatus("PROCESSING");
        batch.setStartedAt(Instant.now());
        batchRepository.save(batch);

        int inserted = userNotificationBulkRepository.insertIgnoreDuplicates(template, batchUserIds);
        batch.setProcessedCount(batchUserIds.size());
        batch.setSuccessCount(inserted);
        batch.setFailedCount(Math.max(0, batchUserIds.size() - inserted));
        batch.setStatus("COMPLETED");
        batch.setFinishedAt(Instant.now());
        batchRepository.save(batch);

        boolean realtimeEnabled = isChannelEnabled(template, "REALTIME");
        boolean emailEnabled = isChannelEnabled(template, "EMAIL");
        for (Long userId : batchUserIds) {
            long unread = userNotificationRepository.countUnreadByUserId(userId);
            unreadCountCacheService.set(userId, unread);
            if (realtimeEnabled) {
                realtimeDispatcher.dispatch(userId, template, unread);
            }
            if (emailEnabled) {
                enqueueEmailDeliveryUseCase.execute(template, userId, emailByUserId.get(userId));
            }
        }

        updateJobProgress(job.getId());
        metrics.increment("batch.completed");
        metrics.increment("inbox.inserted");
        log.info("Completed delivery batch: batchId={}, jobId={}, notificationId={}, expected={}, inserted={}",
                batch.getId(), job.getId(), template.getId(), batchUserIds.size(), inserted);
    }

    @Transactional
    protected void updateJobProgress(Long jobId) {
        NotificationDeliveryJob job = jobRepository.findById(jobId).orElseThrow();
        List<NotificationDeliveryBatch> batches = batchRepository.findByJobId(jobId);
        long processed = batches.stream().mapToLong(b -> value(b.getProcessedCount())).sum();
        long success = batches.stream().mapToLong(b -> value(b.getSuccessCount())).sum();
        long failed = batches.stream().mapToLong(b -> value(b.getFailedCount())).sum();
        boolean done = batches.stream().allMatch(b -> "COMPLETED".equals(b.getStatus()) || "FAILED".equals(b.getStatus()));
        boolean hasFailed = batches.stream().anyMatch(b -> "FAILED".equals(b.getStatus()));

        job.setProcessedTarget(processed);
        job.setSuccessTarget(success);
        job.setFailedTarget(failed);
        if (done) {
            job.setStatus(hasFailed ? "FAILED" : "COMPLETED");
            job.setFinishedAt(Instant.now());
        }
        jobRepository.save(job);
    }

    private boolean markFailed(Long batchId, Exception ex) {
        return batchRepository.findById(batchId).map(batch -> {
            int retryCount = batch.getRetryCount() == null ? 0 : batch.getRetryCount() + 1;
            batch.setRetryCount(retryCount);
            batch.setLastError(ex.getMessage());
            boolean terminalFailure = retryCount >= maxRetry;
            batch.setStatus(terminalFailure ? "FAILED" : "PENDING");
            if ("FAILED".equals(batch.getStatus())) {
                batch.setFinishedAt(Instant.now());
                metrics.increment("batch.failed");
            }
            batchRepository.save(batch);
            updateJobProgress(batch.getJobId());
            return terminalFailure;
        }).orElse(true);
    }

    private List<Long> resolveUserIds(List<NotificationTargetRule> rules) {
        List<Long> userIds = new ArrayList<>();
        for (NotificationTargetRule rule : rules) {
            if (!"USER_IDS".equals(rule.getRuleType())) {
                continue;
            }
            Object rawUserIds = rule.getRulePayload().get("userIds");
            if (rawUserIds instanceof List<?> values) {
                for (Object value : values) {
                    if (value instanceof Number number) {
                        userIds.add(number.longValue());
                    } else if (value != null) {
                        userIds.add(Long.parseLong(String.valueOf(value)));
                    }
                }
            }
        }
        return userIds.stream().distinct().toList();
    }

    @SuppressWarnings("unchecked")
    private Map<Long, String> resolveEmailByUserId(List<NotificationTargetRule> rules) {
        Map<Long, String> result = new HashMap<>();
        for (NotificationTargetRule rule : rules) {
            Object raw = rule.getRulePayload().get("emailByUserId");
            if (raw == null && rule.getRulePayload().get("queryParams") instanceof Map<?, ?> queryParams) {
                raw = queryParams.get("emailByUserId");
            }
            if (raw instanceof Map<?, ?> map) {
                map.forEach((key, value) -> {
                    if (key != null && value != null) {
                        result.put(Long.parseLong(String.valueOf(key)), String.valueOf(value));
                    }
                });
            }
        }
        return result;
    }

    private boolean isChannelEnabled(NotificationTemplate template, String channel) {
        return template.getChannelPayload() != null && Boolean.TRUE.equals(template.getChannelPayload().get(channel));
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private Integer retryCount(Long batchId) {
        return batchRepository.findById(batchId).map(NotificationDeliveryBatch::getRetryCount).orElse(null);
    }

    private void publishDlq(String payload, String sourceKey, String failureStage, Exception ex, Integer retryCount) {
        NotificationDlqEvent dlqEvent = NotificationDlqEvent.builder()
                .sourceTopic("notification.delivery.batch.requested")
                .sourceKey(sourceKey)
                .failureStage(failureStage)
                .payload(payload)
                .errorMessage(ex.getMessage())
                .retryCount(retryCount)
                .occurredAt(Instant.now())
                .build();
        messagePublisher.publish(dlqTopic, sourceKey != null ? sourceKey : UUID.randomUUID().toString(), dlqEvent);
        metrics.increment("dlq.published", "stage", failureStage);
    }
}
