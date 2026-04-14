package com.infinite.user.service.impl;

import com.infinite.common.constant.StatusCode;
import com.infinite.common.dto.request.SearchRequest;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.common.dto.response.PageResponse;
import com.infinite.common.exception.AppException;
import com.infinite.user.dto.request.ChangePasswordRequest;
import com.infinite.user.dto.request.LoginRequest;
import com.infinite.user.dto.request.UserRequest;
import com.infinite.user.dto.response.LoginResponse;
import com.infinite.user.dto.response.UserDto;
import com.infinite.user.repository.UserRepository;
import com.infinite.user.model.User;
import com.infinite.user.service.UserService;
import com.infinite.user.util.Contant;
import com.infinite.user.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

import static com.infinite.common.constant.StatusCode.SUCCESS;
import static com.infinite.common.constant.StatusCode.UNAUTHORIZED;
import static com.infinite.common.dto.response.Response.code;
import static com.infinite.common.dto.response.Response.message;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    JwtUtil jwtUtil;

    @Override
    public ApiResponse<Object> login(LoginRequest request) {

        User user = userRepository.login(request.getUsername())
                .orElseThrow(() -> new AppException(code(UNAUTHORIZED), message("auth.login.fail")));
        validateUser(user, request);
        String token = jwtUtil.generateToken(user.getUsername());
        LoginResponse response = LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .build();

        return ApiResponse.builder()
                .code(StatusCode.SUCCESS.getCode())
                .message(message("auth.login.success"))
                .result(response)
                .build();
    }

    private void validateUser(User user, LoginRequest request) {

        if (user.getLockTime() != null && user.getLockTime().isAfter(LocalDateTime.now())) {
            throw new AppException(
                    StatusCode.LOCKED,
                    Map.of("lockTime", user.getLockTime()));
        }

        if (user.getActive() == Contant.IS_ACTIVE.INACTIVE) {
            throw new AppException(code(UNAUTHORIZED), message("auth.unactive"));
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(code(UNAUTHORIZED), message("auth.login.fail"));
        }
    }

    @Override
    public ApiResponse<Object> getToken() {
        String token = jwtUtil.generateToken();
        return ApiResponse.builder()
                .code(StatusCode.SUCCESS.getCode())
                .message(message(SUCCESS))
                .result(token)
                .build();
    }

    @Override
    public ApiResponse<Object> resetPassword(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(StatusCode.DATA_NOT_EXISTED));
        user.setPassword(passwordEncoder.encode(Contant.PASSWORD_DEFAULT));
        userRepository.save(user);
        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message(SUCCESS))
                .build();
    }

    @Override
    public ApiResponse<Object> changePassword(ChangePasswordRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(StatusCode.DATA_NOT_EXISTED));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException(StatusCode.INVALID_KEY, message("auth.password.invalid"));
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message(SUCCESS))
                .build();
    }

    @Override
    public ApiResponse<Object> create(UserRequest request) {
        ApiResponse<Object> checkRes = performChecks(request);
        if (checkRes != null) {
            return checkRes;
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setCreateBy(request.getNguoithuchien());
        user.setActive(0);

        userRepository.save(user);
        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message(SUCCESS))
                .build();
    }

    @Override
    public ApiResponse<Object> update(UserRequest request) {
        ApiResponse<Object> checkRes = performChecks(request);
        if (checkRes != null) {
            return checkRes;
        }
        User user = userRepository.findById(request.getId())
                        .orElseThrow(() -> new AppException(StatusCode.DATA_NOT_EXISTED));
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setModifiedBy(request.getNguoithuchien());
        user.setActive(0);

        userRepository.save(user);
        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message(SUCCESS))
                .build();
    }

    @Override
    public ApiResponse<Object> searchUsers(SearchRequest request, Pageable pageable) {
        Page<UserDto> data = userRepository.searchUsers(
                request.getSearchKey(),
                request.getStatus(),
                pageable
        );
        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message(SUCCESS))
                .result(new PageResponse<>(
                        data.getContent(),
                        data.getTotalPages(),
                        data.getTotalElements()))
                .build();
    }

    private ApiResponse<Object> performChecks(UserRequest request) {
        ApiResponse<Object> response;
        if (userRepository.existsByUsername(request.getId(), request.getUsername())) {response = existResponse(message("auth.username.exist"));}
        else if (userRepository.existsByEmail(request.getId(), request.getEmail())) {response = existResponse(message("auth.email.exist"));}
        else {response = null;}
        return response;
    }

    private ApiResponse<Object> existResponse(String message) {
        return ApiResponse.builder()
                .code(StatusCode.DATA_EXISTED.getCode())
                .message(message)
                .build();
    }
}
