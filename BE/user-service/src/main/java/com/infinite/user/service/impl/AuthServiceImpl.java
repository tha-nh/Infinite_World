package com.infinite.user.service.impl;

import com.infinite.common.constant.StatusCode;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.common.exception.AppException;
import com.infinite.user.dto.request.LoginRequest;
import com.infinite.user.dto.response.LoginResponse;
import com.infinite.user.model.User;
import com.infinite.user.repository.UserRepository;
import com.infinite.user.service.AuthService;
import com.infinite.user.util.JwtUtil;
import com.infinite.user.util.ValidationUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import static com.infinite.common.constant.StatusCode.SUCCESS;
import static com.infinite.common.constant.StatusCode.UNAUTHORIZED;
import static com.infinite.common.dto.response.Response.code;
import static com.infinite.common.dto.response.Response.message;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    UserRepository userRepository;
    ValidationUtil validationUtil;
    JwtUtil jwtUtil;

    @Override
    public ApiResponse<Object> login(LoginRequest request) {

        User user = userRepository.login(request.getUsername())
                .orElseThrow(() -> new AppException(code(UNAUTHORIZED), message("auth.login.fail")));
        validationUtil.validateUser(user, request);
        String token = jwtUtil.generateToken(user.getUsername());
        LoginResponse response = LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .build();

        return ApiResponse.builder()
                .code(StatusCode.SUCCESS.getCode())
                .message(message(SUCCESS))
                .result(response)
                .build();
    }
}
