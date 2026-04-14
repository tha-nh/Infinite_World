package com.infinite.user.util;

import com.infinite.common.constant.StatusCode;
import com.infinite.common.exception.AppException;
import com.infinite.user.dto.request.LoginRequest;
import com.infinite.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

import static com.infinite.common.constant.StatusCode.UNAUTHORIZED;
import static com.infinite.common.dto.response.Response.code;
import static com.infinite.common.dto.response.Response.message;

@Component
@RequiredArgsConstructor
public class ValidationUtil {

    private final PasswordEncoder passwordEncoder;

    public void validateUser(User user, LoginRequest request) {

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
}