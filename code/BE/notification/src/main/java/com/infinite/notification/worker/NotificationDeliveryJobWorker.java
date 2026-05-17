package com.infinite.notification.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infinite.notification.dto.event.NotificationDlqEvent;
import com.infinite.notification.dto.event.NotificationDeliveryBatchRequestedEvent;
import com.infinite.notification.dto.event.NotificationDeliveryRequestedEvent;
import com.infinite.notification.infrastructure.observability.NotificationMetrics;
import com.infinite.notification.infrastructure.persistence.entity.NotificationDeliveryBatch;
import com.infinite.notification.infrastructure.persistence.entity.NotificationDeliveryJob;
import com.infinite.notification.infrastructure.persistence.entity.NotificationTargetRule;
import com.infinite.notification.infrastructure.persistence.repository.NotificationDeliveryBatchRepository;
import com.infinite.notification.infrastructure.persistence.repository.NotificationDeliveryJobRepository;
import com.infinite.notification.infrastructure.persistence.repository.NotificationTargetRuleRepository;
import com.infinite.notification.messaging.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryJobWorker {

    private final NotificationDeliveryJobRepository jobRepository;
    private final NotificationTargetRuleRepository targetRuleRepository;
    private final NotificationDeliveryBatchRepository batchRepository;
    private final MessagePublisher messagePublisher;
    private final ObjectMapper objectMapper;
    private final NotificationMetrics metrics;

    @Value("${notification.fanout.batch-size:500}")
    private int batchSize;

    @Value("${notification.fanout.max-retry:3}")
    private int maxRetry;

    @Value("${messaging.topics.notification-delivery-batch-requested:notification.delivery.batch.requested}")
    private String batchRequestedTopic;

    @Value("${messaging.topics.notification-dlq:notification.dlq}")
    private String dlqTopic;

    @KafkaListener(
            topics = "${messaging.topics.notification-delivery-requested:notification.delivery.requested}",
            groupId = "${spring.kafka.consumer.group-id:notification-service}",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void consume(@Payload String payload, Acknowledgment acknowledgment) {
        NotificationDeliveryRequestedEvent event = null;
        try {
            event = objectMapper.readValue(payload, NotificationDeliveryRequestedEvent.class);
            process(event);
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Delivery job worker failed: payload={}", payload, ex);
            boolean terminalFailure = event == null;
            if (event != null) {
                terminalFailure = markFailed(event.getJobId(), ex);
            }
            if (terminalFailure) {
                publishDlq(payload, event != null ? String.valueOf(event.getJobId()) : null,
                        "delivery-job", ex, event != null ? retryCount(event.getJobId()) : null);
                acknowledgment.acknowledge();
            }
        }
    }

    @Transactional
    public void process(NotificationDeliveryRequestedEvent event) {
        NotificationDeliveryJob job = jobRepository.findById(event.getJobId()).orElseThrow();
        if ("PROCESSING".equals(job.getStatus()) || "COMPLETED".equals(job.getStatus())) {
            log.info("Skip delivery job in terminal/active state: jobId={}, status={}", job.getId(), job.getStatus());
            return;
        }

        List<NotificationTargetRule> rules = targetRuleRepository.findByNotificationId(job.getNotificationId());
        List<Long> userIds = resolveUserIds(rules);
        if (userIds.isEmpty()) {
            job.setStatus("COMPLETED");
            job.setTotalTarget(0L);
            job.setProcessedTarget(0L);
            job.setSuccessTarget(0L);
            job.setFailedTarget(0L);
            job.setStartedAt(Instant.now());
            job.setFinishedAt(Instant.now());
            jobRepository.save(job);
            log.info("Completed empty delivery job: jobId={}, notificationId={}", job.getId(), job.getNotificationId());
            return;
        }

        job.setStatus("PROCESSING");
        job.setStartedAt(Instant.now());
        job.setTotalTarget((long) userIds.size());
        jobRepository.save(job);

        List<NotificationDeliveryBatch> batches = new ArrayList<>();
        int batchNo = 0;
        for (int start = 0; start < userIds.size(); start += batchSize) {
            int expected = Math.min(batchSize, userIds.size() - start);
            NotificationDeliveryBatch batch = NotificationDeliveryBatch.builder()
                    .jobId(job.getId())
                    .batchNo(batchNo++)
                    .cursorValue((long) start)
                    .status("PENDING")
                    .expectedCount(expected)
                    .processedCount(0)
                    .successCount(0)
                    .failedCount(0)
                    .retryCount(0)
                    .build();
            batches.add(batch);
        }
        batches = batchRepository.saveAll(batches);

        for (NotificationDeliveryBatch batch : batches) {
            NotificationDeliveryBatchRequestedEvent batchEvent = NotificationDeliveryBatchRequestedEvent.builder()
                    .jobId(job.getId())
                    .batchId(batch.getId())
                    .notificationId(job.getNotificationId())
                    .build();
            publishAfterCommit(batchRequestedTopic, String.valueOf(batch.getId()), batchEvent);
        }

        metrics.increment("job.batches.created");
        log.info("Created delivery batches: jobId={}, notificationId={}, totalTarget={}, batchCount={}",
                job.getId(), job.getNotificationId(), userIds.size(), batches.size());
    }

    private List<Long> resolveUserIds(List<NotificationTargetRule> rules) {
        List<Long> userIds = new ArrayList<>();
        for (NotificationTargetRule rule : rules) {
            if (!"USER_IDS".equals(rule.getRuleType())) {
                log.warn("Unsupported target rule for BUOC3 fanout: ruleId={}, ruleType={}",
                        rule.getId(), rule.getRuleType());
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

    @Transactional
    protected boolean markFailed(Long jobId, Exception ex) {
        return jobRepository.findById(jobId).map(job -> {
            int retryCount = job.getRetryCount() == null ? 0 : job.getRetryCount() + 1;
            job.setRetryCount(retryCount);
            job.setLastError(ex.getMessage());
            boolean terminalFailure = retryCount >= maxRetry;
            job.setStatus(terminalFailure ? "FAILED" : "PENDING");
            if ("FAILED".equals(job.getStatus())) {
                job.setFinishedAt(Instant.now());
                metrics.increment("job.failed");
            }
            jobRepository.save(job);
            return terminalFailure;
        }).orElse(true);
    }

    private Integer retryCount(Long jobId) {
        return jobRepository.findById(jobId).map(NotificationDeliveryJob::getRetryCount).orElse(null);
    }

    private void publishDlq(String payload, String sourceKey, String failureStage, Exception ex, Integer retryCount) {
        NotificationDlqEvent dlqEvent = NotificationDlqEvent.builder()
                .sourceTopic("notification.delivery.requested")
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

    private void publishAfterCommit(String topic, String key, Object event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            messagePublisher.publish(topic, key, event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagePublisher.publish(topic, key, event);
            }
        });
    }
}
