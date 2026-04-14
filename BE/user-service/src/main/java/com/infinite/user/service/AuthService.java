package com.infinite.user.service;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.user.dto.request.LoginRequest;

public interface AuthService {
    ApiResponse<Object> login(LoginRequest request);
}
