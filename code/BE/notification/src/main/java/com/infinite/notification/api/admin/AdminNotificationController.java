package com.infinite.notification.api.admin;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.notification.contract.event.NotificationRequestEvent;
import com.infinite.notification.service.NotificationAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationAdminService notificationAdminService;

    @PostMapping
    public ApiResponse<Object> createNotification(
            @Valid @RequestBody NotificationRequestEvent request) {
        return notificationAdminService.create(request);
    }

    @GetMapping
    public ApiResponse<Object> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return notificationAdminService.search(page, size, status);
    }

    @GetMapping("/{id}")
    public ApiResponse<Object> getNotificationById(
            @PathVariable Long id) {
        return notificationAdminService.findById(id);
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Object> cancelNotification(
            @PathVariable Long id) {
        return notificationAdminService.cancel(id);
    }

    @PostMapping("/{id}/retry")
    public ApiResponse<Object> retryNotification(
            @PathVariable Long id) {
        return notificationAdminService.retry(id);
    }

    @GetMapping("/{id}/delivery-summary")
    public ApiResponse<Object> getDeliverySummary(
            @PathVariable Long id) {
        return notificationAdminService.deliverySummary(id);
    }
}
