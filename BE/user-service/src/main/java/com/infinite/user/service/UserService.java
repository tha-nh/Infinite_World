package com.infinite.user.service;

import com.infinite.common.dto.request.SearchRequest;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.user.dto.request.*;
import org.springframework.data.domain.Pageable;

public interface UserService {
    ApiResponse<Object> login(LoginRequest request);
    ApiResponse<Object> getToken();
    ApiResponse<Object> resetPassword(Long id);
    ApiResponse<Object> changePassword(ChangePasswordRequest request);
    ApiResponse<Object> sendForgotPasswordOtp(ForgotPasswordRequest request);
    ApiResponse<Object> verifyForgotPasswordOtp(ForgotPasswordOtpRequest request);
    ApiResponse<Object> sendForgotPasswordSmsOtp(ForgotPasswordSmsRequest request);
    ApiResponse<Object> verifyForgotPasswordSmsOtp(ForgotPasswordSmsOtpRequest request);
    ApiResponse<Object> verifyEmail(VerifyEmailRequest request);
    String verifyEmailHtml(VerifyEmailRequest request);
    ApiResponse<Object> registerWithVerification(RegistrationRequest request);
    ApiResponse<Object> verifyRegistration(VerifyRegistrationRequest request);
    ApiResponse<Object> create(UserRequest request);
    ApiResponse<Object> update(UserRequest request);
    ApiResponse<Object> searchUsers(SearchRequest request, Pageable pageable);
}
