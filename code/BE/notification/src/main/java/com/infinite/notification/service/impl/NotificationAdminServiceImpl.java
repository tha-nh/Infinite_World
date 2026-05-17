package com.infinite.notification.service.impl;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.common.dto.response.PageResponse;
import com.infinite.notification.application.command.AdminNotificationCommand;
import com.infinite.notification.application.command.CreateNotificationRequestUseCase;
import com.infinite.notification.application.query.GetNotificationDetailsQuery;
import com.infinite.notification.contract.event.NotificationRequestEvent;
import com.infinite.notification.dto.response.NotificationDetailDto;
import com.infinite.notification.service.NotificationAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.infinite.common.constant.StatusCode.SUCCESS;
import static com.infinite.common.dto.response.Response.code;
import static com.infinite.common.dto.response.Response.message;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationAdminServiceImpl implements NotificationAdminService {

    private final CreateNotificationRequestUseCase createNotificationRequestUseCase;
    private final AdminNotificationCommand adminNotificationCommand;
    private final GetNotificationDetailsQuery getNotificationDetailsQuery;

    @Override
    public ApiResponse<Object> create(NotificationRequestEvent request) {
        log.info("Admin creating notification: eventId={}", request.getEventId());

        Long notificationId = createNotificationRequestUseCase.execute(request);

        Map<String, Object> response = Map.of(
                "notificationId", notificationId,
                "eventId", request.getEventId(),
                "status", "ACCEPTED"
        );

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("notification.admin.created"))
                .result(response)
                .build();
    }

    @Override
    public ApiResponse<Object> search(int page, int size, String status) {
        log.info("Admin fetching notifications: page={}, size={}, status={}", page, size, status);

        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationDetailDto> result = getNotificationDetailsQuery.execute(pageable, status);

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("notification.admin.list"))
                .result(PageResponse.success(result))
                .build();
    }

    @Override
    public ApiResponse<Object> findById(Long id) {
        log.info("Admin fetching notification: id={}", id);

        NotificationDetailDto notification = getNotificationDetailsQuery.getById(id);

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("notification.admin.detail"))
                .result(notification)
                .build();
    }

    @Override
    public ApiResponse<Object> cancel(Long id) {
        log.info("Admin cancel notification: id={}", id);

        adminNotificationCommand.cancel(id);

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("notification.admin.cancel.success"))
                .build();
    }

    @Override
    public ApiResponse<Object> retry(Long id) {
        log.info("Admin retry notification: id={}", id);

        adminNotificationCommand.retry(id);

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("notification.admin.retry.success"))
                .build();
    }

    @Override
    public ApiResponse<Object> deliverySummary(Long id) {
        log.info("Admin fetching delivery summary: id={}", id);

        Map<String, Object> summary = getNotificationDetailsQuery.getDeliverySummary(id);

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("notification.admin.delivery.summary"))
                .result(summary)
                .build();
    }
}
