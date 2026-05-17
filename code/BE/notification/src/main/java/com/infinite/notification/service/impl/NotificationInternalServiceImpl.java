package com.infinite.notification.service.impl;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.notification.application.command.CreateNotificationRequestUseCase;
import com.infinite.notification.contract.event.NotificationRequestEvent;
import com.infinite.notification.service.NotificationInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.infinite.common.constant.StatusCode.SUCCESS;
import static com.infinite.common.dto.response.Response.code;
import static com.infinite.common.dto.response.Response.message;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationInternalServiceImpl implements NotificationInternalService {

    private final CreateNotificationRequestUseCase createNotificationRequestUseCase;

    @Override
    public ApiResponse<Object> createRequest(NotificationRequestEvent request) {
        log.info("Received notification request: eventId={}, sourceService={}",
                request.getEventId(), request.getSourceService());

        Long notificationId = createNotificationRequestUseCase.execute(request);

        Map<String, Object> response = Map.of(
                "notificationId", notificationId,
                "eventId", request.getEventId(),
                "status", "ACCEPTED"
        );

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("notification.request.accepted"))
                .result(response)
                .build();
    }
}
