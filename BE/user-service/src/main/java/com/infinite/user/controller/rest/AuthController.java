package com.infinite.user.controller.rest;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.user.dto.request.ChangePasswordRequest;
import com.infinite.user.dto.request.ForgotPasswordRequest;
import com.infinite.user.dto.request.ForgotPasswordOtpRequest;
import com.infinite.user.dto.request.ForgotPasswordSmsRequest;
import com.infinite.user.dto.request.ForgotPasswordSmsOtpRequest;
import com.infinite.user.dto.request.LoginRequest;
import com.infinite.user.dto.request.RegistrationRequest;
import com.infinite.user.dto.request.UserRequest;
import com.infinite.user.dto.request.VerifyEmailRequest;
import com.infinite.user.dto.request.VerifyRegistrationRequest;
import com.infinite.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "Đăng ký với xác thực OTP", description = "Đăng ký tài khoản mới với xác thực qua Email hoặc SMS")
    @PostMapping("/register-with-verification")
    public ApiResponse<Object> registerWithVerification(@RequestBody RegistrationRequest request){
        return userService.registerWithVerification(request);
    }

    @Operation(summary = "Xác thực đăng ký", description = "Xác thực OTP và hoàn tất đăng ký tài khoản")
    @PostMapping("/verify-registration")
    public ApiResponse<Object> verifyRegistration(@RequestBody VerifyRegistrationRequest request){
        return userService.verifyRegistration(request);
    }

    @Operation(summary = "Đăng ký tài khoản", description = "Đăng ký tài khoản mới và gửi email xác thực")
    @PostMapping("/register")
    public ApiResponse<Object> register(@RequestBody UserRequest request){
        return userService.create(request);
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

    @Operation(summary = "Quên mật khẩu - Gửi OTP qua SMS", description = "Gửi OTP qua SMS để reset mật khẩu")
    @PostMapping("/forgot-password-sms")
    public ApiResponse<Object> forgotPasswordSms(@RequestBody ForgotPasswordSmsRequest request){
        return userService.sendForgotPasswordSmsOtp(request);
    }

    @Operation(summary = "Xác nhận OTP SMS và đổi mật khẩu", description = "Xác nhận OTP từ SMS và đặt mật khẩu mới")
    @PostMapping("/forgot-password-sms/verify")
    public ApiResponse<Object> verifyForgotPasswordSmsOtp(@RequestBody ForgotPasswordSmsOtpRequest request){
        return userService.verifyForgotPasswordSmsOtp(request);
    }
}
