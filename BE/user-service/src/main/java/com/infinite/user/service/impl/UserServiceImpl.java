package com.infinite.user.service.impl;

import com.infinite.common.constant.StatusCode;
import com.infinite.common.dto.request.SearchRequest;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.common.dto.response.PageResponse;
import com.infinite.common.exception.AppException;
import com.infinite.common.constant.OtpConstant;
import com.infinite.common.service.OtpService;
import com.infinite.notification.service.EmailService;
import com.infinite.notification.service.SmsService;
import com.infinite.notification.service.WebSocketNotificationService;
import com.infinite.user.dto.request.*;
import com.infinite.user.dto.response.LoginResponse;
import com.infinite.user.dto.response.UserDto;
import com.infinite.user.repository.UserRepository;
import com.infinite.user.model.User;
import com.infinite.user.model.Role;
import com.infinite.user.service.RoleService;
import com.infinite.user.service.UserService;
import com.infinite.user.util.Contant;
import com.infinite.user.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.infinite.common.constant.StatusCode.SUCCESS;
import static com.infinite.common.constant.StatusCode.UNAUTHORIZED;
import static com.infinite.common.dto.response.Response.code;
import static com.infinite.common.dto.response.Response.message;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    final UserRepository userRepository;
    final RoleService roleService;
    final PasswordEncoder passwordEncoder;
    final JwtUtil jwtUtil;
    final OtpService otpService;
    final EmailService emailService;
    final SmsService smsService;
    final WebSocketNotificationService webSocketService;

    @org.springframework.beans.factory.annotation.Value("${app.base.url}")
    String appBaseUrl;

    @Override
    public ApiResponse<Object> login(LoginRequest request) {

        User user = userRepository.login(request.getUsername())
                .orElseThrow(() -> new AppException(code(UNAUTHORIZED), message("auth.login.fail")));
        validateUser(user, request);

        Set<String> roleNames = user.getRoles() != null ?
                user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet()) :
                Set.of();

        String token = jwtUtil.generateToken(user.getUsername(), roleNames);
        LoginResponse response = LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .roles(roleNames)
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
    public ApiResponse<Object> sendForgotPasswordOtp(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(StatusCode.DATA_NOT_EXISTED, message("auth.email.notfound")));

        String otp = otpService.generateOtp();
        String otpKey = otpService.generateForgotPasswordOtpKey(request.getEmail());
        otpService.storeOtp(otpKey, otp, 5, TimeUnit.MINUTES);

        emailService.sendOtpEmail(request.getEmail(), otp, "forgot_password");

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("auth.otp.sent"))
                .build();
    }

    @Override
    public ApiResponse<Object> sendForgotPasswordSmsOtp(ForgotPasswordSmsRequest request) {
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new AppException(StatusCode.DATA_NOT_EXISTED, message("auth.phone.notfound")));

        String otp = otpService.generateOtp();
        String otpKey = "forgot_password_otp_sms:" + request.getPhoneNumber();
        otpService.storeOtp(otpKey, otp, 5, TimeUnit.MINUTES);

        smsService.sendOtpSms(request.getPhoneNumber(), otp, "forgot_password");

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("auth.otp.sent"))
                .build();
    }

    @Override
    public ApiResponse<Object> verifyForgotPasswordSmsOtp(ForgotPasswordSmsOtpRequest request) {
        String otpKey = "forgot_password_otp_sms:" + request.getPhoneNumber();

        if (!otpService.verifyOtp(otpKey, request.getOtp())) {
            String storedOtp = otpService.getOtp(otpKey);
            if (storedOtp == null) {
                throw new AppException(StatusCode.INVALID_KEY, message("auth.otp.expired"));
            } else {
                throw new AppException(StatusCode.INVALID_KEY, message("auth.otp.invalid"));
            }
        }

        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new AppException(StatusCode.DATA_NOT_EXISTED, message("auth.phone.notfound")));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        otpService.deleteOtp(otpKey);

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("auth.password.reset.success"))
                .build();
    }

    @Override
    public ApiResponse<Object> verifyForgotPasswordOtp(ForgotPasswordOtpRequest request) {
        // Verify OTP from Redis
        String otpKey = otpService.generateForgotPasswordOtpKey(request.getEmail());

        if (!otpService.verifyOtp(otpKey, request.getOtp())) {
            String storedOtp = otpService.getOtp(otpKey);
            if (storedOtp == null) {
                throw new AppException(StatusCode.INVALID_KEY, message("auth.otp.expired"));
            } else {
                throw new AppException(StatusCode.INVALID_KEY, message("auth.otp.invalid"));
            }
        }

        // Find user and update password
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(StatusCode.DATA_NOT_EXISTED, message("auth.email.notfound")));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        otpService.deleteOtp(otpKey);

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("auth.password.reset.success"))
                .build();
    }

    @Override
    public ApiResponse<Object> verifyEmail(VerifyEmailRequest request) {
        String email = otpService.getOtp("email_verification_token:" + request.getToken());

        if (email == null) {
            throw new AppException(StatusCode.INVALID_KEY, message("auth.token"));
        }

        if ("reject".equals(request.getAction())) {
            otpService.deleteOtp("email_verification_token:" + request.getToken());
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null && user.getActive() == 0) {
                userRepository.delete(user);
            }
            return ApiResponse.builder()
                    .code(code(SUCCESS))
                    .message(message("email.verification.reject.message"))
                    .build();
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(StatusCode.DATA_NOT_EXISTED, message("auth.email.notfound")));

        user.setActive(Contant.IS_ACTIVE.ACTIVE);
        userRepository.save(user);

        otpService.deleteOtp("email_verification_token:" + request.getToken());

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("otp.verification.success"))
                .build();
    }

    @Override
    public String verifyEmailHtml(VerifyEmailRequest request) {
        org.springframework.context.i18n.LocaleContextHolder.setLocale(
                "vi".equals(request.getLang()) ? java.util.Locale.forLanguageTag("vi") : java.util.Locale.ENGLISH
        );

        try {
            String email = otpService.getOtp("email_verification_token:" + request.getToken());

            if (email == null) {
                return buildResultHtml(false, message("auth.token"), request.getLang());
            }

            if ("reject".equals(request.getAction())) {
                otpService.deleteOtp("email_verification_token:" + request.getToken());
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null && user.getActive() == Contant.IS_ACTIVE.INACTIVE) {
                    userRepository.delete(user);
                }
                return buildResultHtml(true, message("email.verification.reject.message"), request.getLang());
            }

            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return buildResultHtml(false, message("auth.email.notfound"), request.getLang());
            }

            // Check if this is a password reset (user was inactive and password is default)
            boolean isPasswordReset = (user.getActive() == Contant.IS_ACTIVE.INACTIVE &&
                    passwordEncoder.matches(Contant.PASSWORD_DEFAULT, user.getPassword()));

            user.setActive(Contant.IS_ACTIVE.ACTIVE);
            userRepository.save(user);

            otpService.deleteOtp("email_verification_token:" + request.getToken());

            if (isPasswordReset) {
                return buildPasswordResetResultHtml(true, message("email.verification.success"),
                        Contant.PASSWORD_DEFAULT, request.getLang());
            } else {
                return buildResultHtml(true, message("email.verification.success"), request.getLang());
            }
        } catch (Exception e) {
            return buildResultHtml(false, message("INTERNAL_ERROR"), request.getLang());
        }
    }

    private String buildResultHtml(boolean success, String message, String lang) {
        org.springframework.context.i18n.LocaleContextHolder.setLocale(
                "vi".equals(lang) ? java.util.Locale.forLanguageTag("vi") : java.util.Locale.ENGLISH
        );

        String title = success ?
                message("email.verification.result.success.title") :
                message("email.verification.result.error.title");
        String subtitle = success ?
                message("email.verification.result.success.subtitle") :
                message("email.verification.result.error.subtitle");
        String copyright = message("email.verification.copyright");

        String gradient = success ?
                "linear-gradient(135deg, #667eea 0%, #764ba2 100%)" :
                "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)";

        String iconSvg = success ?
                "<svg viewBox=\"0 0 24 24\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\" style=\"width: 100px; height: 100px;\">" +
                        "    <circle cx=\"12\" cy=\"12\" r=\"10\" stroke=\"white\" stroke-width=\"2\" fill=\"none\"/>" +
                        "    <path d=\"M8 12L11 15L16 9\" stroke=\"white\" stroke-width=\"2.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>" +
                        "</svg>" :
                "<svg viewBox=\"0 0 24 24\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\" style=\"width: 100px; height: 100px;\">" +
                        "    <circle cx=\"12\" cy=\"12\" r=\"10\" stroke=\"white\" stroke-width=\"2\" fill=\"none\"/>" +
                        "    <path d=\"M8 8L16 16M16 8L8 16\" stroke=\"white\" stroke-width=\"2.5\" stroke-linecap=\"round\"/>" +
                        "</svg>";

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "    <title>" + title + "</title>" +
                "    <style>" +
                "        * { margin: 0; padding: 0; box-sizing: border-box; }" +
                "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: " + gradient + "; min-height: 100vh; display: flex; justify-content: center; align-items: center; padding: 20px; }" +
                "        .container { max-width: 500px; width: 100%; background: white; border-radius: 20px; box-shadow: 0 20px 60px rgba(0,0,0,0.3); overflow: hidden; animation: slideUp 0.5s ease-out; }" +
                "        @keyframes slideUp { from { opacity: 0; transform: translateY(30px); } to { opacity: 1; transform: translateY(0); } }" +
                "        .header { background: " + gradient + "; padding: 50px 30px; text-align: center; position: relative; }" +
                "        .icon-container { margin-bottom: 20px; animation: scaleIn 0.6s ease-out 0.2s both; }" +
                "        @keyframes scaleIn { from { opacity: 0; transform: scale(0); } to { opacity: 1; transform: scale(1); } }" +
                "        .header h1 { color: white; font-size: 32px; margin-bottom: 10px; font-weight: 600; }" +
                "        .header p { color: rgba(255,255,255,0.9); font-size: 16px; }" +
                "        .content { padding: 40px 30px; text-align: center; }" +
                "        .message-box { background: #f8f9fa; padding: 25px; border-radius: 15px; border-left: 4px solid " + (success ? "#667eea" : "#f5576c") + "; }" +
                "        .message-box p { color: #495057; font-size: 16px; line-height: 1.6; }" +
                "        .footer { padding: 20px 30px; background: #f8f9fa; text-align: center; border-top: 1px solid #e9ecef; }" +
                "        .footer p { color: #6c757d; font-size: 14px; }" +
                "        .decorative-circle { position: absolute; border-radius: 50%; background: rgba(255,255,255,0.1); }" +
                "        .circle-1 { width: 100px; height: 100px; top: -50px; right: -50px; }" +
                "        .circle-2 { width: 150px; height: 150px; bottom: -75px; left: -75px; }" +
                "        @media only screen and (max-width: 600px) {" +
                "            .header h1 { font-size: 26px; }" +
                "            .content { padding: 30px 20px; }" +
                "        }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class=\"container\">" +
                "        <div class=\"header\">" +
                "            <div class=\"decorative-circle circle-1\"></div>" +
                "            <div class=\"decorative-circle circle-2\"></div>" +
                "            <div class=\"icon-container\">" +
                "                " + iconSvg +
                "            </div>" +
                "            <h1>" + title + "</h1>" +
                "            <p>" + subtitle + "</p>" +
                "        </div>" +
                "        <div class=\"content\">" +
                "            <div class=\"message-box\">" +
                "                <p>" + message + "</p>" +
                "            </div>" +
                "        </div>" +
                "        <div class=\"footer\">" +
                "            <p>" + copyright + "</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }

    @Override
    public ApiResponse<Object> registerWithVerification(RegistrationRequest request) {
        ApiResponse<Object> checkRes = performChecksForRegistration(request);
        if (checkRes != null) {
            return checkRes;
        }

        String otp = otpService.generateOtp();
        String otpKey;

        if ("sms".equals(request.getVerificationMethod())) {
            otpKey = "registration_otp_sms:" + request.getPhoneNumber();
            otpService.storeOtp(otpKey, otp, OtpConstant.REGISTRATION_OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);
            smsService.sendOtpSms(request.getPhoneNumber(), otp, "registration");
        } else {
            otpKey = "registration_otp_email:" + request.getEmail();
            otpService.storeOtp(otpKey, otp, OtpConstant.REGISTRATION_OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);
            emailService.sendOtpEmail(request.getEmail(), otp, "registration");
        }

        // Store registration data temporarily
        String tempKey = "temp_registration:" + otp;
        otpService.storeOtp(tempKey,
                String.format("%s|%s|%s|%s|%s|%s",
                        request.getUsername(), request.getPassword(), request.getName(),
                        request.getEmail(), request.getPhoneNumber(), request.getNguoithuchien()),
                OtpConstant.REGISTRATION_OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("otp.registration.sent"))
                .build();
    }

    @Override
    public ApiResponse<Object> verifyRegistration(VerifyRegistrationRequest request) {
        String otpKey;

        if ("sms".equals(request.getVerificationMethod())) {
            otpKey = "registration_otp_sms:" + request.getIdentifier();
        } else {
            otpKey = "registration_otp_email:" + request.getIdentifier();
        }

        if (!otpService.verifyOtp(otpKey, request.getOtp())) {
            String storedOtp = otpService.getOtp(otpKey);
            if (storedOtp == null) {
                throw new AppException(StatusCode.INVALID_KEY, message("auth.otp.expired"));
            } else {
                throw new AppException(StatusCode.INVALID_KEY, message("auth.otp.invalid"));
            }
        }

        // Get registration data
        String tempKey = "temp_registration:" + request.getOtp();
        String tempData = otpService.getOtp(tempKey);
        if (tempData == null) {
            throw new AppException(StatusCode.INVALID_KEY, message("auth.otp.expired"));
        }

        String[] parts = tempData.split("\\|");

        User user = new User();
        user.setUsername(parts[0]);
        user.setPassword(passwordEncoder.encode(parts[1]));
        user.setName(parts[2]);
        user.setEmail(parts[3]);
        user.setPhoneNumber(parts[4]);
        user.setCreateBy(parts[5]);
        user.setActive(Contant.IS_ACTIVE.ACTIVE);

        userRepository.save(user);

        // Assign USER role by default
        Role userRole = roleService.findByName("USER");
        if (userRole != null) {
            roleService.assignRoleToUser(user.getId(), userRole.getId());
        }

        // Clean up
        otpService.deleteOtp(otpKey);
        otpService.deleteOtp(tempKey);

        // Send welcome notification
        webSocketService.sendToUser(user.getId().toString(), "welcome",
                "Chào mừng!", "Tài khoản đã được tạo thành công");

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("auth.register.success"))
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
        user.setActive(Contant.IS_ACTIVE.INACTIVE);

        userRepository.save(user);

        // Assign roles if provided by admin, otherwise assign USER role by default
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            for (Long roleId : request.getRoleIds()) {
                roleService.assignRoleToUser(user.getId(), roleId);
            }
        } else {
            // Auto-assign USER role for public registration
            Role userRole = roleService.findByName("USER");
            if (userRole != null) {
                roleService.assignRoleToUser(user.getId(), userRole.getId());
            }
        }

        String verificationToken = otpService.generateOtp();
        otpService.storeOtp("email_verification_token:" + verificationToken, request.getEmail(),
                OtpConstant.EMAIL_VERIFICATION_OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        String verificationUrl = appBaseUrl + "/v1/api/auth/verify-email?token=" + verificationToken;
        emailService.sendVerificationEmail(request.getEmail(), verificationUrl);

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("auth.register.success"))
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

        // Store old active status to check if changed
        Integer oldActive = user.getActive();

        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setModifiedBy(request.getNguoithuchien());

        // Handle password update
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Handle active status change
        if (request.getActive() != null) {
            user.setActive(request.getActive());

            // If admin sets active = 0, reset password and send verification email
            if (request.getActive() == Contant.IS_ACTIVE.INACTIVE && !request.getActive().equals(oldActive)) {
                user.setPassword(passwordEncoder.encode(Contant.PASSWORD_DEFAULT));

                String verificationToken = otpService.generateOtp();
                otpService.storeOtp("email_verification_token:" + verificationToken, user.getEmail(),
                        OtpConstant.EMAIL_VERIFICATION_OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);

                String verificationUrl = appBaseUrl + "/v1/api/auth/verify-email?token=" + verificationToken;
                emailService.sendPasswordResetVerificationEmail(user.getEmail(), verificationUrl, Contant.PASSWORD_DEFAULT);
            }
        }

        userRepository.save(user);

        // Update roles if provided by admin
        if (request.getRoleIds() != null) {
            // Clear existing roles
            if (user.getRoles() != null) {
                user.getRoles().clear();
                userRepository.save(user);
            }

            // Assign new roles
            for (Long roleId : request.getRoleIds()) {
                roleService.assignRoleToUser(user.getId(), roleId);
            }
        }

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message(SUCCESS))
                .build();
    }

    @Override
    public ApiResponse<Object> searchUsers(SearchRequest request, Pageable pageable) {
        Page<UserDto> dataWithRoles = userRepository.searchUsers(
                request.getSearchKey(),
                request.getStatus(),
                pageable
        );

        // Load roles for each user
        Page<UserDto> data = dataWithRoles.map(userDto -> {
            User user = userRepository.findById(userDto.getId()).orElse(null);
            if (user != null && user.getRoles() != null) {
                List<String> roleNames = user.getRoles().stream()
                        .map(role -> role.getName())
                        .toList();
                userDto.setRoles(roleNames);
            }
            return userDto;
        });

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
            response = existResponse(message("auth.username.exist"));
        } else if (userRepository.existsByEmail(request.getId(), request.getEmail())) {
            response = existResponse(message("auth.email.exist"));
        } else {
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

    private ApiResponse<Object> performChecksForRegistration(RegistrationRequest request) {
        ApiResponse<Object> response;
        if (userRepository.existsByUsername(null, request.getUsername())) {
            response = existResponse(message("auth.username.exist"));
        } else if (request.getEmail() != null && userRepository.existsByEmail(null, request.getEmail())) {
            response = existResponse(message("auth.email.exist"));
        } else {
            response = null;
        }
        return response;
    }

    @Override
    public ApiResponse<Object> lockUser(LockUserRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(StatusCode.DATA_NOT_EXISTED, message("auth.user.notfound")));

        user.setActive(Contant.IS_ACTIVE.LOCKED); // LOCKED
        user.setLockTime(request.getLockTime());
        user.setModifiedBy(request.getNguoithuchien());

        userRepository.save(user);

        String lockMessage = request.getLockTime() != null
                ? message("user.lock.temporary") + " " + request.getLockTime()
                : message("user.lock.permanent");

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(lockMessage)
                .build();
    }

    @Override
    public ApiResponse<Object> unlockUser(Long userId, String nguoithuchien) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(StatusCode.DATA_NOT_EXISTED, message("auth.user.notfound")));

        user.setActive(Contant.IS_ACTIVE.ACTIVE); // ACTIVE
        user.setLockTime(null);
        user.setModifiedBy(nguoithuchien);

        userRepository.save(user);

        return ApiResponse.builder()
                .code(code(SUCCESS))
                .message(message("user.unlock.success"))
                .build();
    }

    private String buildPasswordResetResultHtml(boolean success, String message, String defaultPassword, String lang) {
        org.springframework.context.i18n.LocaleContextHolder.setLocale(
                "vi".equals(lang) ? java.util.Locale.forLanguageTag("vi") : java.util.Locale.ENGLISH
        );

        String title = success ?
                message("email.verification.result.success.title") :
                message("email.verification.result.error.title");
        String subtitle = success ?
                message("password.reset.result.subtitle") :
                message("email.verification.result.error.subtitle");
        String copyright = message("email.verification.copyright");
        String passwordLabel = message("password.reset.default.password");
        String warningTitle = message("password.reset.warning.title");
        String warningContent = message("password.reset.warning.content");

        String gradient = success ?
                "linear-gradient(135deg, #667eea 0%, #764ba2 100%)" :
                "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)";

        String iconSvg = success ?
                "<svg viewBox=\"0 0 24 24\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\" style=\"width: 100px; height: 100px;\">" +
                        "    <circle cx=\"12\" cy=\"12\" r=\"10\" stroke=\"white\" stroke-width=\"2\" fill=\"none\"/>" +
                        "    <path d=\"M8 12L11 15L16 9\" stroke=\"white\" stroke-width=\"2.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>" +
                        "</svg>" :
                "<svg viewBox=\"0 0 24 24\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\" style=\"width: 100px; height: 100px;\">" +
                        "    <circle cx=\"12\" cy=\"12\" r=\"10\" stroke=\"white\" stroke-width=\"2\" fill=\"none\"/>" +
                        "    <path d=\"M8 8L16 16M16 8L8 16\" stroke=\"white\" stroke-width=\"2.5\" stroke-linecap=\"round\"/>" +
                        "</svg>";

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "    <title>" + title + "</title>" +
                "    <style>" +
                "        * { margin: 0; padding: 0; box-sizing: border-box; }" +
                "        body { font-family: Arial, sans-serif; background: " + gradient + "; min-height: 100vh; display: flex; justify-content: center; align-items: center; padding: 20px; }" +
                "        .container { max-width: 500px; width: 100%; background: white; border-radius: 15px; box-shadow: 0 10px 30px rgba(0,0,0,0.2); overflow: hidden; }" +
                "        .header { background: " + gradient + "; padding: 40px 30px; text-align: center; }" +
                "        .icon-container { margin-bottom: 20px; }" +
                "        .header h1 { color: white; font-size: 28px; margin-bottom: 10px; font-weight: 600; }" +
                "        .header p { color: rgba(255,255,255,0.9); font-size: 16px; }" +
                "        .content { padding: 30px; text-align: center; }" +
                "        .message-box { background: #f8f9fa; padding: 20px; border-radius: 10px; margin-bottom: 25px; }" +
                "        .message-box p { color: #495057; font-size: 16px; line-height: 1.6; }" +
                "        .password-box { background: #e3f2fd; border: 2px solid #2196f3; border-radius: 10px; padding: 20px; margin: 20px 0; }" +
                "        .password-label { color: #1976d2; font-size: 14px; font-weight: bold; margin-bottom: 10px; }" +
                "        .password { font-size: 32px; font-weight: bold; color: #1976d2; font-family: monospace; letter-spacing: 2px; }" +
                "        .warning-box { background: #fff3cd; border: 1px solid #ffeaa7; border-radius: 8px; padding: 15px; margin: 20px 0; text-align: left; }" +
                "        .warning-title { color: #856404; font-weight: bold; margin-bottom: 8px; }" +
                "        .warning-content { color: #856404; font-size: 14px; line-height: 1.5; }" +
                "        .footer { padding: 20px 30px; background: #f8f9fa; text-align: center; border-top: 1px solid #e9ecef; }" +
                "        .footer p { color: #6c757d; font-size: 14px; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class=\"container\">" +
                "        <div class=\"header\">" +
                "            <div class=\"icon-container\">" +
                "                " + iconSvg +
                "            </div>" +
                "            <h1>" + title + "</h1>" +
                "            <p>" + subtitle + "</p>" +
                "        </div>" +
                "        <div class=\"content\">" +
                "            <div class=\"message-box\">" +
                "                <p>" + message + "</p>" +
                "            </div>" +
                (success ?
                        "            <div class=\"password-box\">" +
                                "                <div class=\"password-label\">" + passwordLabel + ":</div>" +
                                "                <div class=\"password\">" + defaultPassword + "</div>" +
                                "            </div>" +
                                "            <div class=\"warning-box\">" +
                                "                <div class=\"warning-title\">⚠️ " + warningTitle + "</div>" +
                                "                <div class=\"warning-content\">" + warningContent + "</div>" +
                                "            </div>" : "") +
                "        </div>" +
                "        <div class=\"footer\">" +
                "            <p>" + copyright + "</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }
}