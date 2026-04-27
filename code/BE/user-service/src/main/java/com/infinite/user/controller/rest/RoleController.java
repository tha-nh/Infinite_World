package com.infinite.user.controller.rest;

import com.infinite.common.dto.request.SearchRequest;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.user.dto.request.RoleRequest;
import com.infinite.user.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "APIs for managing roles")
public class RoleController {
    
    private final RoleService roleService;
    
    @PostMapping
    @Operation(summary = "Create new role")
    public ResponseEntity<ApiResponse<Object>> create(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(roleService.create(request));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update role")
    public ResponseEntity<ApiResponse<Object>> update(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        request.setId(id);
        return ResponseEntity.ok(roleService.update(request));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete role")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.delete(id));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID")
    public ResponseEntity<ApiResponse<Object>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.findById(id));
    }
    
    @PostMapping("/search")
    @Operation(summary = "Search roles")
    public ResponseEntity<ApiResponse<Object>> search(@RequestBody SearchRequest request, Pageable pageable) {
        return ResponseEntity.ok(roleService.searchRoles(request, pageable));
    }
    
    @PostMapping("/assign/{userId}/{roleId}")
    @Operation(summary = "Assign role to user")
    public ResponseEntity<ApiResponse<Object>> assignRoleToUser(@PathVariable Long userId, @PathVariable Long roleId) {
        return ResponseEntity.ok(roleService.assignRoleToUser(userId, roleId));
    }
    
    @DeleteMapping("/remove/{userId}/{roleId}")
    @Operation(summary = "Remove role from user")
    public ResponseEntity<ApiResponse<Object>> removeRoleFromUser(@PathVariable Long userId, @PathVariable Long roleId) {
        return ResponseEntity.ok(roleService.removeRoleFromUser(userId, roleId));
    }
}