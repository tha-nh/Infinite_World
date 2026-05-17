package com.infinite.notification.application.query;

import com.infinite.common.exception.NotFoundException;
import com.infinite.common.util.I18n;
import com.infinite.notification.dto.response.NotificationDetailDto;
import com.infinite.notification.infrastructure.persistence.entity.NotificationDeliveryJob;
import com.infinite.notification.infrastructure.persistence.entity.NotificationTemplate;
import com.infinite.notification.infrastructure.persistence.repository.NotificationDeliveryJobRepository;
import com.infinite.notification.infrastructure.persistence.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetNotificationDetailsQuery {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationDeliveryJobRepository deliveryJobRepository;

    @Transactional(readOnly = true)
    public Page<NotificationDetailDto> execute(Pageable pageable, String status) {
        log.debug("Fetching notifications: status={}", status);
        
        Page<NotificationTemplate> templates;
        if (status != null && !status.isEmpty()) {
            templates = templateRepository.findByStatusPaginated(status, pageable);
        } else {
            templates = templateRepository.findAll(pageable);
        }
        
        return templates.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public NotificationDetailDto getById(Long id) {
        log.debug("Fetching notification by id: id={}", id);
        
        NotificationTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(I18n.msg("notification.not.found")));
        
        return toDto(template);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDeliverySummary(Long notificationId) {
        log.debug("Fetching delivery summary: notificationId={}", notificationId);
        
        List<NotificationDeliveryJob> jobs = deliveryJobRepository.findByNotificationId(notificationId);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("notificationId", notificationId);
        summary.put("totalJobs", jobs.size());
        
        long totalTarget = jobs.stream().mapToLong(NotificationDeliveryJob::getTotalTarget).sum();
        long processedTarget = jobs.stream().mapToLong(NotificationDeliveryJob::getProcessedTarget).sum();
        long successTarget = jobs.stream().mapToLong(NotificationDeliveryJob::getSuccessTarget).sum();
        long failedTarget = jobs.stream().mapToLong(NotificationDeliveryJob::getFailedTarget).sum();
        
        summary.put("totalTarget", totalTarget);
        summary.put("processedTarget", processedTarget);
        summary.put("successTarget", successTarget);
        summary.put("failedTarget", failedTarget);
        summary.put("jobs", jobs.stream().map(this::jobToMap).toList());
        
        return summary;
    }

    private NotificationDetailDto toDto(NotificationTemplate entity) {
        return NotificationDetailDto.builder()
            .id(entity.getId())
            .requestId(entity.getRequestId())
            .code(entity.getCode())
            .title(entity.getTitle())
            .body(entity.getBody())
            .type(entity.getType())
            .priority(entity.getPriority())
            .imageUrl(entity.getImageUrl())
            .actionPayload(entity.getActionPayload())
            .rewardPayload(entity.getRewardPayload())
            .channelPayload(entity.getChannelPayload())
            .startAt(entity.getStartAt())
            .expireAt(entity.getExpireAt())
            .status(entity.getStatus())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    private Map<String, Object> jobToMap(NotificationDeliveryJob job) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", job.getId());
        map.put("jobType", job.getJobType());
        map.put("status", job.getStatus());
        map.put("totalTarget", job.getTotalTarget());
        map.put("processedTarget", job.getProcessedTarget());
        map.put("successTarget", job.getSuccessTarget());
        map.put("failedTarget", job.getFailedTarget());
        map.put("retryCount", job.getRetryCount());
        map.put("startedAt", job.getStartedAt());
        map.put("finishedAt", job.getFinishedAt());
        return map;
    }
}
