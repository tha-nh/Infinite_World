package com.infinite.core.service;

import com.infinite.common.dto.request.SearchRequest;
import com.infinite.common.dto.response.ApiResponse;
import org.springframework.data.domain.Pageable;

public interface UserService {
    ApiResponse<Object> create(UserRequest request);
    ApiResponse<Object> searchUsers(SearchRequest request, Pageable pageable);
}
