package com.infinite.user.service.impl;

import com.infinite.common.constant.StatusCode;
import com.infinite.common.dto.request.SearchRequest;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.common.dto.response.PageResponse;
import com.infinite.common.exception.AppException;
import com.infinite.user.dto.request.RoleRequest;
import com.infinite.user.dto.response.RoleDto;
import com.infinite.user.model.Role;
import com.infinite.user.model.User;
import com.infinite.user.repository.RoleRepository;
import com.infinite.user.repository.UserRepository;
import com.infinite.user.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

import static com.infinite.common.constant.StatusCode.SUCCESS;
import static com.infinite.common.dto.response.Response.code;
import static com.infinite.common.dto.response.Response.message;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    
    @Override
    public ApiResponse<Object> create(RoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            return ApiResponse.builder()
                    .code(StatusCode.DATA_EXISTED.getCode())
                    .message(message("role.name.exist"))
                    .build();
        }
        
        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(request.getNguoithuchien())
                .build();
        
        roleRepository.save(role);
        
        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("role.create.success"))
                .build();
    }
    
    @Override
    public ApiResponse<Object> update(RoleRequest request) {
        Role role = roleRepository.findById(request.getId())
                .orElseThrow(() -> new AppException(StatusCode.DATA_NOT_EXISTED, message("role.notfound")));
        
        if (!role.getName().equals(request.getName()) && roleRepository.existsByName(request.getName())) {
            return ApiResponse.builder()
                    .code(StatusCode.DATA_EXISTED.getCode())
                    .message(message("role.name.exist"))
                    .build();
        }
        
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setUpdatedBy(request.getNguoithuchien());
        
        roleRepository.save(role);
        
        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("role.update.success"))
                .build();
    }
    
    @Override
    public ApiResponse<Object> delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new AppException(StatusCode.DATA_NOT_EXISTED, message("role.notfound")));
        
        roleRepository.delete(role);
        
        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("role.delete.success"))
                .build();
    }
    
    @Override
    public ApiResponse<Object> findById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new AppException(StatusCode.DATA_NOT_EXISTED, message("role.notfound")));
        
        RoleDto roleDto = RoleDto.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .createdBy(role.getCreatedBy())
                .updatedBy(role.getUpdatedBy())
                .build();
        
        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message(SUCCESS))
                .result(roleDto)
                .build();
    }
    
    @Override
    public Role findByName(String name) {
        return roleRepository.findByName(name).orElse(null);
    }
    
    @Override
    public ApiResponse<Object> searchRoles(SearchRequest request, Pageable pageable) {
        Page<Role> roles = roleRepository.findAll(pageable);
        
        Page<RoleDto> roleDtos = roles.map(role -> RoleDto.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .createdBy(role.getCreatedBy())
                .updatedBy(role.getUpdatedBy())
                .build());
        
        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message(SUCCESS))
                .result(new PageResponse<>(
                        roleDtos.getContent(),
                        roleDtos.getTotalPages(),
                        roleDtos.getTotalElements()))
                .build();
    }
    
    @Override
    @Transactional
    public ApiResponse<Object> assignRoleToUser(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(StatusCode.DATA_NOT_EXISTED, message("auth.user.notfound")));
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(StatusCode.DATA_NOT_EXISTED, message("role.notfound")));
        
        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }
        
        user.getRoles().add(role);
        userRepository.save(user);
        
        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("role.assign.success"))
                .build();
    }
    
    @Override
    @Transactional
    public ApiResponse<Object> removeRoleFromUser(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(StatusCode.DATA_NOT_EXISTED, message("auth.user.notfound")));
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(StatusCode.DATA_NOT_EXISTED, message("role.notfound")));
        
        if (user.getRoles() != null) {
            user.getRoles().remove(role);
            userRepository.save(user);
        }
        
        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("role.remove.success"))
                .build();
    }
}