package com.infinite.notification.service;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.notification.contract.event.NotificationRequestEvent;

public interface NotificationInternalService {

    ApiResponse<Object> createRequest(NotificationRequestEvent request);
}
