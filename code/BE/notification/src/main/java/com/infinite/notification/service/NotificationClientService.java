package com.infinite.notification.service;

import com.infinite.common.dto.response.ApiResponse;

public interface NotificationClientService {

    ApiResponse<Object> getInbox(String userIdHeader, int page, int size);

    ApiResponse<Object> getUnreadCount(String userIdHeader);

    ApiResponse<Object> markAsRead(String userIdHeader, Long id);

    ApiResponse<Object> markAllAsRead(String userIdHeader);

    ApiResponse<Object> delete(String userIdHeader, Long id);

    ApiResponse<Object> claim(String userIdHeader, Long id);
}
