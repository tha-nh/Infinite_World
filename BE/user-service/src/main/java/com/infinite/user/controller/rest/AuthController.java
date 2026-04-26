package com.infinite.user.controller.rest;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.user.dto.request.*;
import com.infinite.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(path = "v1/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Authentication", description = "APIs xác thực và quản lý mật khẩu")
public class AuthController {
    UserService userService;

    @Operation(summary = "Đăng nhập", description = "Đăng nhập bằng username và password")
    @PostMapping("/login")
    public ApiResponse<Object> login(@RequestBody LoginRequest request){
        return userService.login(request);
    }

    @Operation(summary = "Đăng ký tài khoản", description = "Đăng ký tài khoản mới và gửi OTP qua email")
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Object> register(
            @RequestParam(value = "request") String requestJson,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        UserRequest request = objectMapper.readValue(requestJson, UserRequest.class);
        return userService.create(request, avatar);
    }

    @Operation(summary = "Xác thực đăng ký", description = "Xác thực token và kích hoạt/từ chối tài khoản")
    @GetMapping(value = "/verify-registration", produces = "text/html")
    public String verifyRegistration(
            @RequestParam String token,
            @RequestParam String action,
            @RequestParam(required = false, defaultValue = "vi") String lang){
        VerifyRegistrationTokenRequest request = new VerifyRegistrationTokenRequest();
        request.setToken(token);
        request.setAction(action);
        request.setLang(lang);
        return userService.verifyRegistrationToken(request);
    }

    @Operation(summary = "Xác thực email", description = "Xác thực email thông qua token trong email")
    @GetMapping(value = "/verify-email", produces = "text/html")
    public String verifyEmailHtml(
            @RequestParam String token,
            @RequestParam(required = false) String action,
            @RequestParam(required = false, defaultValue = "en") String lang){
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken(token);
        request.setAction(action);
        request.setLang(lang);
        return userService.verifyEmailHtml(request);
    }

    @Operation(summary = "Lấy token", description = "Lấy token hiện tại")
    @GetMapping("/get-token")
    public ApiResponse<Object> getToken(){
        return userService.getToken();
    }

    @Operation(summary = "Đổi mật khẩu", description = "Đổi mật khẩu (Public API - không cần authentication)")
    @PostMapping("/change-password")
    public ApiResponse<Object> changePassword(@RequestBody ChangePasswordRequest request){
        return userService.changePassword(request);
    }

    @Operation(summary = "Quên mật khẩu - Gửi OTP", description = "Gửi OTP qua email để reset mật khẩu")
    @PostMapping("/forgot-password")
    public ApiResponse<Object> forgotPassword(@RequestBody ForgotPasswordRequest request){
        return userService.sendForgotPasswordOtp(request);
    }

    @Operation(summary = "Xác nhận OTP và đổi mật khẩu", description = "Xác nhận OTP và đặt mật khẩu mới")
    @PostMapping("/forgot-password/verify")
    public ApiResponse<Object> verifyForgotPasswordOtp(@RequestBody ForgotPasswordOtpRequest request){
        return userService.verifyForgotPasswordOtp(request);
    }
}
