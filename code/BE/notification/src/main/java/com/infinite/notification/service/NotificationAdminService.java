package com.infinite.notification.service;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.notification.contract.event.NotificationRequestEvent;

public interface NotificationAdminService {

    ApiResponse<Object> create(NotificationRequestEvent request);

    ApiResponse<Object> search(int page, int size, String status);

    ApiResponse<Object> findById(Long id);

    ApiResponse<Object> cancel(Long id);

    ApiResponse<Object> retry(Long id);

    ApiResponse<Object> deliverySummary(Long id);
}
