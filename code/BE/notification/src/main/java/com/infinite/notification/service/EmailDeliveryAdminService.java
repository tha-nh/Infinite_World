package com.infinite.notification.service;

import com.infinite.common.dto.response.ApiResponse;

public interface EmailDeliveryAdminService {

    ApiResponse<Object> search(int page, int size, String status);

    ApiResponse<Object> findById(Long id);

    ApiResponse<Object> retry(Long id);
}
