package com.infinite.user.service;

import com.infinite.common.dto.request.SearchRequest;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.user.dto.request.ChangePasswordRequest;
import com.infinite.user.dto.request.LoginRequest;
import com.infinite.user.dto.request.UserRequest;
import org.springframework.data.domain.Pageable;

public interface UserService {
    ApiResponse<Object> login(LoginRequest request);
    ApiResponse<Object> getToken();
    ApiResponse<Object> resetPassword(Long id);
    ApiResponse<Object> changePassword(ChangePasswordRequest request);
    ApiResponse<Object> create(UserRequest request);
    ApiResponse<Object> update(UserRequest request);
    ApiResponse<Object> searchUsers(SearchRequest request, Pageable pageable);
}
