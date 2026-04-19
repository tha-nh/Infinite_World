package com.infinite.user.service;

import com.infinite.common.dto.request.SearchRequest;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.user.dto.request.RoleRequest;
import org.springframework.data.domain.Pageable;

public interface RoleService {
    
    ApiResponse<Object> create(RoleRequest request);
    
    ApiResponse<Object> update(RoleRequest request);
    
    ApiResponse<Object> delete(Long id);
    
    ApiResponse<Object> findById(Long id);
    
    ApiResponse<Object> searchRoles(SearchRequest request, Pageable pageable);
    
    ApiResponse<Object> assignRoleToUser(Long userId, Long roleId);
    
    ApiResponse<Object> removeRoleFromUser(Long userId, Long roleId);
}