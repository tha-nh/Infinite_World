package com.infinite.notification.service.impl;

import com.infinite.common.dto.event.EmailNotificationEvent;
import com.infinite.common.constant.EmailType;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.common.dto.response.PageResponse;
import com.infinite.common.exception.NotFoundException;
import com.infinite.common.util.I18n;
import com.infinite.notification.infrastructure.persistence.entity.EmailDeliveryLog;
import com.infinite.notification.infrastructure.persistence.repository.EmailDeliveryLogRepository;
import com.infinite.notification.messaging.MessagePublisher;
import com.infinite.notification.service.EmailDeliveryAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;

import static com.infinite.common.constant.StatusCode.SUCCESS;
import static com.infinite.common.dto.response.Response.code;
import static com.infinite.common.dto.response.Response.message;

@Service
@RequiredArgsConstructor
public class EmailDeliveryAdminServiceImpl implements EmailDeliveryAdminService {

    private final EmailDeliveryLogRepository emailDeliveryLogRepository;
    private final MessagePublisher messagePublisher;

    @Value("${messaging.topics.email:notification.email}")
    private String emailTopic;

    @Override
    public ApiResponse<Object> search(int page, int size, String status) {
        Page<EmailDeliveryLog> result = emailDeliveryLogRepository.findByStatusOptional(status, PageRequest.of(page, size));
        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("notification.email.delivery.list"))
                .result(PageResponse.success(result))
                .build();
    }

    @Override
    public ApiResponse<Object> findById(Long id) {
        EmailDeliveryLog log = findLog(id);
        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("notification.email.delivery.detail"))
                .result(log)
                .build();
    }

    @Override
    public ApiResponse<Object> retry(Long id) {
        EmailDeliveryLog log = findLog(id);
        log.setStatus("PENDING");
        log.setErrorMessage(null);
        emailDeliveryLogRepository.save(log);

        Map<String, Object> metadata = new HashMap<>();
        if (log.getNotificationId() != null) {
            metadata.put("notificationId", log.getNotificationId());
        }

        EmailNotificationEvent event = EmailNotificationEvent.builder()
                .eventId(log.getEventId())
                .to(log.getToEmail())
                .userId(log.getUserId() == null ? null : String.valueOf(log.getUserId()))
                .emailType(resolveEmailType(log))
                .variables(resolveVariables(log))
                .locale(resolveLocale(log))
                .subject(log.getSubject())
                .content(log.getPayload() == null ? null : String.valueOf(log.getPayload().getOrDefault("body", "")))
                .isHtml(false)
                .metadata(metadata)
                .build();
        messagePublisher.publish(emailTopic, log.getEventId(), event);

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("notification.email.delivery.retry.success"))
                .build();
    }

    private EmailDeliveryLog findLog(Long id) {
        return emailDeliveryLogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(I18n.msg("notification.email.delivery.not.found")));
    }

    private String resolveLocale(EmailDeliveryLog log) {
        Object raw = log.getPayload() == null ? null : log.getPayload().get("locale");
        return raw == null ? "en" : String.valueOf(raw);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveVariables(EmailDeliveryLog log) {
        if (log.getPayload() != null && log.getPayload().get("variables") instanceof Map<?, ?> map) {
            Map<String, Object> variables = new HashMap<>();
            map.forEach((key, value) -> {
                if (key != null) {
                    variables.put(String.valueOf(key), value);
                }
            });
            return variables;
        }
        return Map.of();
    }

    private EmailType resolveEmailType(EmailDeliveryLog log) {
        Object raw = resolveVariables(log).get("emailType");
        return raw == null ? null : EmailType.valueOf(String.valueOf(raw));
    }
}
