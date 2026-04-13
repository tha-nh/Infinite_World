package com.infinite.user.service.impl;

import com.infinite.common.constant.StatusCode;
import com.infinite.common.dto.request.SearchRequest;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.common.dto.response.PageResponse;
import com.infinite.common.exception.AppException;
import com.infinite.common.util.I18n;
import com.infinite.common.util.MessageUtils;
import com.infinite.user.dto.request.UserRequest;
import com.infinite.user.dto.response.UserDto;
import com.infinite.user.repository.UserRepository;
import com.infinite.user.model.User;
import com.infinite.user.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.infinite.common.constant.StatusCode.SUCCESS;
import static com.infinite.common.dto.response.Response.code;
import static com.infinite.common.dto.response.Response.message;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;

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

        if (userRepository.existsByUsername(request.getId(), request.getUsername())) {
            response = existResponse(I18n.msg("auth.username.exist"));
        }
        else if (userRepository.existsByEmail(request.getId(), request.getEmail())) {
            response = existResponse(I18n.msg("auth.email.exist"));
        }
        else {
            response = null;
        }

        return response;
    }

    private ApiResponse<Object> existResponse(String message) {
        return ApiResponse.builder()
                .code(StatusCode.DATA_EXISTED.getCode())
                .message(message)
                .build();
    }
}
