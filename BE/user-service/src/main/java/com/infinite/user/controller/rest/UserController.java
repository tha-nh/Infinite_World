package com.infinite.user.controller.rest;

import com.infinite.common.dto.request.SearchRequest;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.user.dto.request.ChangePasswordRequest;
import com.infinite.user.dto.request.UserRequest;
import com.infinite.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "v1/api/user", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "User Management", description = "APIs quản lý user (ADMIN only)")
public class UserController {
    UserService userService;

    @Operation(summary = "Tìm kiếm users", description = "Tìm kiếm và phân trang users")
    @PostMapping("/search")
    public ApiResponse<Object> search(@RequestBody SearchRequest request, Pageable pageable){
        return userService.searchUsers(request, pageable);
    }

    @Operation(summary = "Tạo user mới", description = "Tạo user mới (ADMIN)")
    @PostMapping("/create")
    public ApiResponse<Object> create(@RequestBody UserRequest request){
        return userService.create(request);
    }

    @Operation(summary = "Cập nhật user", description = "Cập nhật thông tin user (ADMIN)")
    @PostMapping("/update")
    public ApiResponse<Object> update(@RequestBody UserRequest request){
        return userService.update(request);
    }

    @Operation(summary = "Đổi mật khẩu user", description = "Admin đổi mật khẩu cho user")
    @PostMapping("/change-password")
    public ApiResponse<Object> changePassword(@RequestBody ChangePasswordRequest request){
        return userService.changePassword(request);
    }

    @Operation(summary = "Reset mật khẩu về mặc định", description = "ADMIN reset mật khẩu user về mặc định")
    @PostMapping("/reset-password/{userId}")
    public ApiResponse<Object> resetPasswordToDefault(@PathVariable Long userId){
        return userService.resetPassword(userId);
    }
}
