package com.infinite.user.service;

import com.infinite.common.dto.request.SearchRequest;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.user.dto.request.*;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

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
    ApiResponse<Object> create(UserRequest request, MultipartFile avatar);
    ApiResponse<Object> update(UserRequest request, MultipartFile avatar);
    ApiResponse<Object> searchUsers(SearchRequest request, Pageable pageable);
    
    ApiResponse<Object> lockUser(LockUserRequest request);
    
    ApiResponse<Object> unlockUser(Long userId, String nguoithuchien);
    
    ApiResponse<Object> uploadAvatar(Long userId, MultipartFile file);
}
