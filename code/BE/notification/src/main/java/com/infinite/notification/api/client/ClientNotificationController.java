package com.infinite.notification.api.client;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.notification.service.NotificationClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Client API for user inbox operations.
 * 
 * IMPORTANT: This API expects X-USER-ID to be set by the gateway after JWT validation.
 * This service only handles business logic.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class ClientNotificationController {

    private final NotificationClientService notificationClientService;

    @GetMapping
    public ApiResponse<Object> getInbox(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-USER-ID") String userIdHeader) {
        return notificationClientService.getInbox(userIdHeader, page, size);
    }

    @GetMapping("/unread-count")
    public ApiResponse<Object> getUnreadCount(
            @RequestHeader("X-USER-ID") String userIdHeader) {
        return notificationClientService.getUnreadCount(userIdHeader);
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Object> markAsRead(
            @PathVariable Long id,
            @RequestHeader("X-USER-ID") String userIdHeader) {
        return notificationClientService.markAsRead(userIdHeader, id);
    }

    @PostMapping("/read-all")
    public ApiResponse<Object> markAllAsRead(
            @RequestHeader("X-USER-ID") String userIdHeader) {
        return notificationClientService.markAllAsRead(userIdHeader);
    }

    @PostMapping("/{id}/delete")
    public ApiResponse<Object> deleteNotification(
            @PathVariable Long id,
            @RequestHeader("X-USER-ID") String userIdHeader) {
        return notificationClientService.delete(userIdHeader, id);
    }

    @PostMapping("/{id}/claim")
    public ApiResponse<Object> claimReward(
            @PathVariable Long id,
            @RequestHeader("X-USER-ID") String userIdHeader) {
        return notificationClientService.claim(userIdHeader, id);
    }
}
