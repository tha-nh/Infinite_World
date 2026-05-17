package com.infinite.notification.api.internal;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.notification.contract.event.NotificationRequestEvent;
import com.infinite.notification.service.NotificationInternalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final NotificationInternalService notificationInternalService;

    @PostMapping("/requests")
    public ApiResponse<Object> createNotificationRequest(
            @Valid @RequestBody NotificationRequestEvent request) {
        return notificationInternalService.createRequest(request);
    }
}
