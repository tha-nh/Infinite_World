package com.infinite.user.service;

import com.infinite.common.constant.EmailType;
import com.infinite.common.constant.OtpConstant;
import com.infinite.notification.contract.builder.NotificationRequestBuilder;
import com.infinite.notification.contract.dto.NotificationContent;
import com.infinite.notification.contract.dto.NotificationTarget;
import com.infinite.notification.contract.enumtype.NotificationChannel;
import com.infinite.notification.contract.enumtype.NotificationPriority;
import com.infinite.notification.contract.enumtype.NotificationTargetType;
import com.infinite.notification.contract.enumtype.NotificationType;
import com.infinite.notification.contract.event.NotificationRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Publishes user-service notification requests through the unified notification contract.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPublisher {

    private static final String SOURCE_SERVICE = "user-service";
    private static final String SOURCE_MODULE = "user";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${notification.topics.request:notification.request.v1}")
    private String notificationRequestTopic;

    @Value("${app.base.url:http://localhost:8080}")
    private String appBaseUrl;

    public void publishNotificationRequest(NotificationRequestEvent event) {
        try {
            kafkaTemplate.send(notificationRequestTopic, event.getIdempotencyKey(), event);
            log.debug("Published notification request to topic: {} for target: {}",
                    notificationRequestTopic, event.getTarget());
        } catch (Exception e) {
            log.error("Failed to publish notification request event", e);
            throw new RuntimeException("Failed to publish notification request", e);
        }
    }

    public void sendOtpEmail(String email, String userId, String otp, String type) {
        EmailType emailType = switch (type) {
            case "forgot_password" -> EmailType.FORGOT_PASSWORD_OTP;
            case "registration" -> EmailType.REGISTRATION_OTP;
            case "login" -> EmailType.LOGIN_OTP;
            default -> EmailType.FORGOT_PASSWORD_OTP;
        };

        Map<String, Object> variables = new HashMap<>();
        variables.put("otp", otp);
        variables.put("expirationMinutes", getExpirationMinutes(emailType));
        variables.put("emailType", emailType.name());

        String title = switch (emailType) {
            case REGISTRATION_OTP -> "Registration verification code";
            case LOGIN_OTP -> "Login verification code";
            default -> "Password reset verification code";
        };
        String body = "Your verification code is " + otp + ". It expires in "
                + getExpirationMinutes(emailType) + " minutes.";

        publishEmailRequest(email, userId, "send_otp_email", stableKey("otp", type, userId, otp),
                title, body, variables);
    }

    public void sendWebSocketToUser(String userId, String type, String title, String message) {
        NotificationRequestEvent event = baseBuilder("send_realtime_to_user",
                stableKey("realtime", type, userId, UUID.randomUUID().toString()))
                .channels(Set.of(NotificationChannel.INBOX, NotificationChannel.REALTIME))
                .target(userTarget(userId, null))
                .content(content(NotificationType.ACCOUNT, title, message, Map.of("realtimeType", type)))
                .metadata(Map.of("realtimeType", type))
                .build();

        publishNotificationRequest(event);
    }

    public void broadcastWebSocket(String type, String title, String message) {
        NotificationRequestEvent event = baseBuilder("broadcast_realtime",
                stableKey("broadcast", type, UUID.randomUUID().toString()))
                .channels(Set.of(NotificationChannel.INBOX, NotificationChannel.REALTIME))
                .target(NotificationTarget.builder()
                        .type(NotificationTargetType.ALL)
                        .queryParams(Map.of("broadcast", true))
                        .build())
                .content(content(NotificationType.ANNOUNCEMENT, title, message, Map.of("realtimeType", type)))
                .metadata(Map.of("realtimeType", type, "broadcast", true))
                .build();

        publishNotificationRequest(event);
    }

    public void sendAccountVerificationEmail(String email, String userId, String username, String verificationToken) {
        String locale = currentLocale();
        String approveUrl = appBaseUrl + "/v1/api/auth/verify-registration?token=" + verificationToken
                + "&action=approve&lang=" + locale;
        String rejectUrl = appBaseUrl + "/v1/api/auth/verify-registration?token=" + verificationToken
                + "&action=reject&lang=" + locale;

        Map<String, Object> variables = new HashMap<>();
        variables.put("emailType", EmailType.REGISTRATION_VERIFICATION.name());
        variables.put("username", username);
        variables.put("approveUrl", approveUrl);
        variables.put("rejectUrl", rejectUrl);

        String body = "Registration request for " + username + ". Approve: " + approveUrl
                + " Reject: " + rejectUrl;
        publishEmailRequest(email, userId, "send_account_verification_email",
                stableKey("registration-verification", userId, verificationToken),
                "Account registration verification", body, variables);
    }

    public void sendUserStatusChangeNotification(String email, String userId, String username,
                                                 String action, LocalDateTime lockTime, String performedBy) {
        EmailType emailType = switch (action) {
            case "LOCKED" -> EmailType.USER_LOCKED;
            case "UNLOCKED" -> EmailType.USER_UNLOCKED;
            case "AUTO_UNLOCKED" -> EmailType.USER_AUTO_UNLOCKED;
            case "UPDATED" -> EmailType.USER_UPDATED;
            default -> throw new IllegalArgumentException("Unknown action: " + action);
        };

        Map<String, Object> variables = new HashMap<>();
        variables.put("emailType", emailType.name());
        variables.put("username", username);
        variables.put("action", action);
        if (lockTime != null) {
            variables.put("lockTime", lockTime);
        }
        if (performedBy != null) {
            variables.put("performedBy", performedBy);
        }

        String body = "Hello " + username + ", your account status was changed to " + action + ".";
        publishEmailRequest(email, userId, "send_user_status_email",
                stableKey("user-status", action, userId, String.valueOf(lockTime), String.valueOf(performedBy)),
                "Account status updated", body, variables);
    }

    public void sendPasswordResetVerificationEmail(String email, String userId, String verificationUrl) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("emailType", EmailType.PASSWORD_RESET_VERIFICATION.name());
        variables.put("verificationUrl", verificationUrl);

        String body = "Use this link to verify your password reset request: " + verificationUrl;
        publishEmailRequest(email, userId, "send_password_reset_verification_email",
                stableKey("password-reset-verification", userId, verificationUrl),
                "Password reset verification", body, variables);
    }

    public void sendLoginAlertEmail(String email, String userId, String username,
                                    String ipAddress, String device) {
        LocalDateTime loginTime = LocalDateTime.now();
        Map<String, Object> variables = new HashMap<>();
        variables.put("emailType", EmailType.LOGIN_ALERT.name());
        variables.put("username", username);
        variables.put("loginTime", loginTime);
        variables.put("ipAddress", ipAddress != null ? ipAddress : "Unknown");
        variables.put("device", device != null ? device : "Unknown device");

        String body = "Hello " + username + ", your account signed in from "
                + variables.get("device") + " at " + loginTime + " using IP " + variables.get("ipAddress") + ".";
        publishEmailRequest(email, userId, "send_login_alert_email",
                stableKey("login-alert", userId, loginTime.toString()),
                "New login detected", body, variables);
    }

    private void publishEmailRequest(String email, String userId, String sourceAction, String idempotencyKey,
                                     String title, String body, Map<String, Object> variables) {
        NotificationRequestEvent event = baseBuilder(sourceAction, idempotencyKey)
                .channels(Set.of(NotificationChannel.INBOX, NotificationChannel.EMAIL))
                .target(userTarget(userId, email))
                .content(content(NotificationType.ACCOUNT, title, body, variables))
                .metadata(variables)
                .build();

        publishNotificationRequest(event);
    }

    private NotificationRequestBuilder baseBuilder(String sourceAction, String idempotencyKey) {
        return NotificationRequestBuilder.create()
                .sourceService(SOURCE_SERVICE)
                .sourceModule(SOURCE_MODULE)
                .sourceAction(sourceAction)
                .requestId(UUID.randomUUID().toString())
                .idempotencyKey(idempotencyKey);
    }

    private NotificationContent content(NotificationType type, String title, String body, Map<String, Object> variables) {
        return NotificationContent.builder()
                .type(type)
                .priority(NotificationPriority.NORMAL)
                .title(title)
                .body(body)
                .locale(currentLocale())
                .templateVars(variables)
                .build();
    }

    private NotificationTarget userTarget(String userId, String email) {
        Long parsedUserId = Long.parseLong(userId);
        Map<String, Object> queryParams = new HashMap<>();
        if (email != null && !email.isBlank()) {
            queryParams.put("emailByUserId", Map.of(String.valueOf(parsedUserId), email));
        }

        return NotificationTarget.builder()
                .type(NotificationTargetType.USER_IDS)
                .userIds(List.of(parsedUserId))
                .queryParams(queryParams)
                .build();
    }

    private int getExpirationMinutes(EmailType emailType) {
        return switch (emailType) {
            case LOGIN_OTP -> (int) OtpConstant.LOGIN_OTP_EXPIRATION_MINUTES;
            case REGISTRATION_OTP -> (int) OtpConstant.REGISTRATION_OTP_EXPIRATION_MINUTES;
            case FORGOT_PASSWORD_OTP -> (int) OtpConstant.DEFAULT_OTP_EXPIRATION_MINUTES;
            default -> (int) OtpConstant.DEFAULT_OTP_EXPIRATION_MINUTES;
        };
    }

    private String stableKey(String... parts) {
        return String.join(":", parts);
    }

    private String currentLocale() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String acceptLanguage = attributes.getRequest().getHeader("Accept-Language");
            if (acceptLanguage != null && !acceptLanguage.isBlank()) {
                return acceptLanguage.split(",")[0].trim().split("-")[0];
            }
        }
        return LocaleContextHolder.getLocale().getLanguage();
    }
}
