package com.infinite.notification.application.command;

import com.infinite.common.dto.event.EmailNotificationEvent;
import com.infinite.common.constant.EmailType;
import com.infinite.notification.infrastructure.persistence.entity.EmailDeliveryLog;
import com.infinite.notification.infrastructure.persistence.entity.NotificationTemplate;
import com.infinite.notification.infrastructure.persistence.repository.EmailDeliveryLogRepository;
import com.infinite.notification.messaging.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnqueueEmailDeliveryUseCase {

    private final EmailDeliveryLogRepository emailDeliveryLogRepository;
    private final MessagePublisher messagePublisher;

    @Value("${messaging.topics.email:notification.email}")
    private String emailTopic;

    @Transactional
    public void execute(NotificationTemplate template, Long userId, String toEmail) {
        if (!StringUtils.hasText(toEmail)) {
            log.debug("Skip email delivery without recipient: notificationId={}, userId={}", template.getId(), userId);
            return;
        }

        String eventId = "email-" + template.getId() + "-" + userId;
        if (emailDeliveryLogRepository.findByEventId(eventId).isPresent()) {
            log.debug("Skip duplicate email delivery: eventId={}", eventId);
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("title", template.getTitle());
        payload.put("body", template.getBody());
        payload.put("action", template.getActionPayload());
        payload.put("reward", template.getRewardPayload());
        payload.put("locale", resolveLocale(template));
        payload.put("variables", resolveVariables(template));

        EmailDeliveryLog logEntry = EmailDeliveryLog.builder()
                .eventId(eventId)
                .notificationId(template.getId())
                .userId(userId)
                .toEmail(toEmail)
                .subject(template.getTitle())
                .payload(payload)
                .status("PENDING")
                .retryCount(0)
                .build();
        emailDeliveryLogRepository.save(logEntry);

        EmailNotificationEvent event = EmailNotificationEvent.builder()
                .eventId(eventId)
                .to(toEmail)
                .userId(String.valueOf(userId))
                .emailType(resolveEmailType(template))
                .variables(resolveVariables(template))
                .locale(resolveLocale(template))
                .subject(template.getTitle())
                .content(template.getBody())
                .isHtml(false)
                .metadata(resolveMetadata(template))
                .build();
        publishAfterCommit(emailTopic, eventId, event);
        log.info("Enqueued email delivery: eventId={}, notificationId={}, userId={}", eventId, template.getId(), userId);
    }

    private String resolveLocale(NotificationTemplate template) {
        Object raw = template.getChannelPayload() == null ? null : template.getChannelPayload().get("_locale");
        return StringUtils.hasText(raw == null ? null : String.valueOf(raw)) ? String.valueOf(raw) : "en";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveVariables(NotificationTemplate template) {
        if (template.getChannelPayload() != null) {
            Object raw = template.getChannelPayload().get("_templateVars");
            if (raw instanceof Map<?, ?> map) {
                Map<String, Object> variables = new HashMap<>();
                map.forEach((key, value) -> {
                    if (key != null) {
                        variables.put(String.valueOf(key), value);
                    }
                });
                return variables;
            }
        }
        return Map.of();
    }

    private EmailType resolveEmailType(NotificationTemplate template) {
        Object raw = resolveVariables(template).get("emailType");
        if (!StringUtils.hasText(raw == null ? null : String.valueOf(raw))) {
            return null;
        }
        return EmailType.valueOf(String.valueOf(raw));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveMetadata(NotificationTemplate template) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("notificationId", template.getId());
        if (template.getChannelPayload() != null && template.getChannelPayload().get("_metadata") instanceof Map<?, ?> map) {
            map.forEach((key, value) -> {
                if (key != null) {
                    metadata.put(String.valueOf(key), value);
                }
            });
        }
        return metadata;
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
