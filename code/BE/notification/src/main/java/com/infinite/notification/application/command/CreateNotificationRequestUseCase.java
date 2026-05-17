package com.infinite.notification.application.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infinite.notification.contract.event.NotificationRequestEvent;
import com.infinite.notification.dto.event.NotificationDeliveryRequestedEvent;
import com.infinite.notification.infrastructure.observability.NotificationMetrics;
import com.infinite.notification.infrastructure.persistence.entity.*;
import com.infinite.notification.infrastructure.persistence.repository.*;
import com.infinite.notification.messaging.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateNotificationRequestUseCase {

    private final NotificationRequestRepository requestRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationTargetRuleRepository targetRuleRepository;
    private final NotificationDeliveryJobRepository deliveryJobRepository;
    private final MessagePublisher messagePublisher;
    private final NotificationMetrics metrics;
    private final ObjectMapper objectMapper;

    @Value("${messaging.topics.notification-delivery-requested:notification.delivery.requested}")
    private String deliveryRequestedTopic;

    @Transactional
    public Long execute(NotificationRequestEvent event) {
        log.info("Processing notification request: eventId={}, idempotencyKey={}", 
            event.getEventId(), event.getIdempotencyKey());
        metrics.increment("request.received", "sourceService", event.getSourceService());

        // Check idempotency
        if (requestRepository.existsByIdempotencyKey(event.getIdempotencyKey())) {
            log.warn("Duplicate request detected: eventId={}, requestId={}, traceId={}, idempotencyKey={}",
                event.getEventId(), event.getRequestId(), event.getTraceId(), event.getIdempotencyKey());
            metrics.increment("request.duplicate", "sourceService", event.getSourceService());
            var existingRequest = requestRepository.findByIdempotencyKey(event.getIdempotencyKey())
                .orElseThrow();
            var existingTemplate = templateRepository.findByRequestId(existingRequest.getId())
                .stream().findFirst().orElse(null);
            return existingTemplate != null ? existingTemplate.getId() : null;
        }

        // 1. Persist notification request
        NotificationRequest request = NotificationRequest.builder()
            .eventId(event.getEventId())
            .requestId(event.getRequestId())
            .traceId(event.getTraceId())
            .sourceService(event.getSourceService())
            .sourceModule(event.getSourceModule())
            .sourceAction(event.getSourceAction())
            .schemaVersion(event.getSchemaVersion())
            .idempotencyKey(event.getIdempotencyKey())
            .requestPayload(convertToMap(event))
            .status("ACCEPTED")
            .build();
        request = requestRepository.save(request);
        log.info("Saved notification request: requestDbId={}, eventId={}, requestId={}, traceId={}",
            request.getId(), event.getEventId(), event.getRequestId(), event.getTraceId());

        // 2. Create notification template
        NotificationTemplate template = NotificationTemplate.builder()
            .requestId(request.getId())
            .code(event.getContent().getType() != null ? event.getContent().getType().name() : null)
            .title(event.getContent().getTitle())
            .body(event.getContent().getBody())
            .type(event.getContent().getType() != null ? event.getContent().getType().name() : "SYSTEM")
            .priority(event.getContent().getPriority() != null ? (short) event.getContent().getPriority().getLevel() : 0)
            .imageUrl(event.getContent().getImageUrl())
            .actionPayload(event.getAction() != null ? convertToMap(event.getAction()) : null)
            .rewardPayload(event.getReward() != null ? convertToMap(event.getReward()) : null)
            .channelPayload(convertChannelsToMap(event))
            .status("PENDING")
            .build();
        template = templateRepository.save(template);
        log.info("Saved notification template: notificationId={}, requestDbId={}, eventId={}",
            template.getId(), request.getId(), event.getEventId());

        // 3. Create target rule
        NotificationTargetRule targetRule = NotificationTargetRule.builder()
            .notificationId(template.getId())
            .ruleType(event.getTarget().getType() != null ? event.getTarget().getType().name() : "USER_IDS")
            .rulePayload(convertToMap(event.getTarget()))
            .build();
        targetRuleRepository.save(targetRule);
        log.info("Saved target rule: notificationId={}, ruleType={}", template.getId(), targetRule.getRuleType());

        // 4. Create delivery job
        NotificationDeliveryJob job = NotificationDeliveryJob.builder()
            .notificationId(template.getId())
            .jobType("INBOX_FANOUT")
            .status("PENDING")
            .totalTarget(0L)
            .processedTarget(0L)
            .successTarget(0L)
            .failedTarget(0L)
            .retryCount(0)
            .scheduledAt(Instant.now())
            .build();
        job = deliveryJobRepository.save(job);
        log.info("Saved delivery job: jobId={}, notificationId={}, eventId={}, traceId={}",
            job.getId(), template.getId(), event.getEventId(), event.getTraceId());
        metrics.increment("job.created");

        NotificationDeliveryRequestedEvent deliveryEvent = NotificationDeliveryRequestedEvent.builder()
            .jobId(job.getId())
            .notificationId(template.getId())
            .requestId(request.getId())
            .traceId(event.getTraceId())
            .build();
        publishAfterCommit(deliveryRequestedTopic, String.valueOf(job.getId()), deliveryEvent);
        log.info("Registered delivery requested event after commit: topic={}, jobId={}, notificationId={}",
            deliveryRequestedTopic, job.getId(), template.getId());

        return template.getId();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object obj) {
        return objectMapper.convertValue(obj, Map.class);
    }

    private Map<String, Object> convertChannelsToMap(NotificationRequestEvent event) {
        Map<String, Object> channelMap = new HashMap<>();
        if (event.getChannels() != null) {
            event.getChannels().forEach(channel -> channelMap.put(channel.name(), true));
        }
        if (event.getContent() != null) {
            channelMap.put("_locale", event.getContent().getLocale());
            channelMap.put("_templateVars", event.getContent().getTemplateVars());
        }
        channelMap.put("_metadata", event.getMetadata());
        return channelMap;
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
