package com.infinite.user.service;

import com.infinite.common.constant.EmailType;
import com.infinite.common.constant.OtpConstant;
import com.infinite.common.dto.event.AccountVerificationEvent;
import com.infinite.common.dto.event.EmailNotificationEvent;
import com.infinite.common.dto.event.SmsNotificationEvent;
import com.infinite.common.dto.event.UserStatusChangeEvent;
import com.infinite.common.dto.event.WebSocketNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for publishing notification events to Kafka
 * User service sends events, notification service consumes them
 * NO FALLBACK - Kafka only!
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${notification.topics.email:notification.email}")
    private String emailTopic;
    
    @Value("${notification.topics.sms:notification.sms}")
    private String smsTopic;
    
    @Value("${notification.topics.websocket:notification.websocket}")
    private String websocketTopic;
    
    @Value("${app.base.url:http://localhost:8080}")
    private String appBaseUrl;
    
    /**
     * Publish email notification event
     */
    public void publishEmailNotification(EmailNotificationEvent event) {
        try {
            kafkaTemplate.send(emailTopic, event.getUserId(), event);
            log.debug("Published email notification to topic: {} for user: {}", emailTopic, event.getUserId());
        } catch (Exception e) {
            log.error("Failed to publish email notification event", e);
            throw new RuntimeException("Failed to publish email notification", e);
        }
    }
    
    /**
     * Publish SMS notification event
     */
    public void publishSmsNotification(SmsNotificationEvent event) {
        try {
            kafkaTemplate.send(smsTopic, event.getUserId(), event);
            log.debug("Published SMS notification to topic: {} for user: {}", smsTopic, event.getUserId());
        } catch (Exception e) {
            log.error("Failed to publish SMS notification event", e);
            throw new RuntimeException("Failed to publish SMS notification", e);
        }
    }
    
    /**
     * Publish WebSocket notification event
     */
    public void publishWebSocketNotification(WebSocketNotificationEvent event) {
        try {
            String key = event.isBroadcast() ? "broadcast" : event.getUserId();
            kafkaTemplate.send(websocketTopic, key, event);
            log.debug("Published WebSocket notification to topic: {}", websocketTopic);
        } catch (Exception e) {
            log.error("Failed to publish WebSocket notification event", e);
            throw new RuntimeException("Failed to publish WebSocket notification", e);
        }
    }
    
    // ========== Helper methods (NEW UNIFIED APPROACH) ==========
    
    /**
     * Helper: Send OTP email using new unified EmailType approach
     */
    public void sendOtpEmail(String email, String userId, String otp, String type) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        
        // Map old type string to new EmailType enum
        EmailType emailType = switch (type) {
            case "forgot_password" -> EmailType.FORGOT_PASSWORD_OTP;
            case "registration" -> EmailType.REGISTRATION_OTP;
            case "login" -> EmailType.LOGIN_OTP;
            default -> EmailType.FORGOT_PASSWORD_OTP;
        };
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("otp", otp);
        variables.put("expirationMinutes", getExpirationMinutes(emailType));
        
        EmailNotificationEvent event = EmailNotificationEvent.builder()
            .emailType(emailType)
            .to(email)
            .userId(userId)
            .variables(variables)
            .locale(locale)
            .build();
        
        publishEmailNotification(event);
    }
    
    private int getExpirationMinutes(EmailType emailType) {
        return switch (emailType) {
            case LOGIN_OTP -> (int) OtpConstant.LOGIN_OTP_EXPIRATION_MINUTES;
            case REGISTRATION_OTP -> (int) OtpConstant.REGISTRATION_OTP_EXPIRATION_MINUTES;
            case FORGOT_PASSWORD_OTP -> (int) OtpConstant.DEFAULT_OTP_EXPIRATION_MINUTES;
            default -> (int) OtpConstant.DEFAULT_OTP_EXPIRATION_MINUTES;
        };
    }
    
    /**
     * Helper: Send OTP SMS
     */
    public void sendOtpSms(String phoneNumber, String userId, String otp, String type) {
        SmsNotificationEvent event = SmsNotificationEvent.builder()
            .phoneNumber(phoneNumber)
            .userId(userId)
            .metadata(Map.of("type", type, "otp", otp))
            .build();
        
        publishSmsNotification(event);
    }
    
    /**
     * Helper: Send WebSocket notification to user
     */
    public void sendWebSocketToUser(String userId, String type, String title, String message) {
        WebSocketNotificationEvent event = WebSocketNotificationEvent.builder()
            .userId(userId)
            .type(type)
            .title(title)
            .message(message)
            .broadcast(false)
            .build();
        
        publishWebSocketNotification(event);
    }
    
    /**
     * Helper: Broadcast WebSocket notification
     */
    public void broadcastWebSocket(String type, String title, String message) {
        WebSocketNotificationEvent event = WebSocketNotificationEvent.builder()
            .type(type)
            .title(title)
            .message(message)
            .broadcast(true)
            .build();
        
        publishWebSocketNotification(event);
    }
    // ========== New unified methods using EmailType ==========
    
    /**
     * Helper: Send account verification email using new unified approach
     */
    public void sendAccountVerificationEmail(String email, String userId, String username, String verificationToken) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        
        // Build approve and reject URLs using injected appBaseUrl from config
        String approveUrl = appBaseUrl + "/v1/api/auth/verify-registration?token=" + verificationToken + "&action=approve&lang=" + locale;
        String rejectUrl = appBaseUrl + "/v1/api/auth/verify-registration?token=" + verificationToken + "&action=reject&lang=" + locale;
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("approveUrl", approveUrl);
        variables.put("rejectUrl", rejectUrl);
        
        EmailNotificationEvent event = EmailNotificationEvent.builder()
            .emailType(EmailType.REGISTRATION_VERIFICATION)
            .to(email)
            .userId(userId)
            .variables(variables)
            .locale(locale)
            .build();
        
        publishEmailNotification(event);
        log.debug("Published registration verification email for user: {} with locale: {}", userId, locale);
    }
    
    /**
     * Helper: Send user status change notification using new unified approach
     */
    public void sendUserStatusChangeNotification(String email, String userId, String username, 
                                                String action, LocalDateTime lockTime, String performedBy) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        
        // Map action to EmailType
        EmailType emailType = switch (action) {
            case "LOCKED" -> EmailType.USER_LOCKED;
            case "UNLOCKED" -> EmailType.USER_UNLOCKED;
            case "AUTO_UNLOCKED" -> EmailType.USER_AUTO_UNLOCKED;
            case "UPDATED" -> EmailType.USER_UPDATED;
            default -> throw new IllegalArgumentException("Unknown action: " + action);
        };
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        if (lockTime != null) {
            variables.put("lockTime", lockTime);
        }
        if (performedBy != null) {
            variables.put("performedBy", performedBy);
        }
        
        EmailNotificationEvent event = EmailNotificationEvent.builder()
            .emailType(emailType)
            .to(email)
            .userId(userId)
            .variables(variables)
            .locale(locale)
            .build();
        
        publishEmailNotification(event);
        log.debug("Published user status change email for user: {} action: {} with locale: {}", userId, action, locale);
    }
    
    /**
     * Helper: Send password reset verification email using new unified approach
     */
    public void sendPasswordResetVerificationEmail(String email, String userId, String verificationUrl) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("verificationUrl", verificationUrl);
        
        EmailNotificationEvent event = EmailNotificationEvent.builder()
            .emailType(EmailType.PASSWORD_RESET_VERIFICATION)
            .to(email)
            .userId(userId)
            .variables(variables)
            .locale(locale)
            .build();
        
        publishEmailNotification(event);
        log.debug("Published password reset verification email for user: {} with locale: {}", userId, locale);
    }
    
    /**
     * Helper: Send login alert email
     */
    public void sendLoginAlertEmail(String email, String userId, String username, 
                                   String ipAddress, String device) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("loginTime", LocalDateTime.now()); // Send raw LocalDateTime, let notification format it
        variables.put("ipAddress", ipAddress != null ? ipAddress : "Unknown");
        variables.put("device", device != null ? device : "Unknown device");
        
        EmailNotificationEvent event = EmailNotificationEvent.builder()
            .emailType(EmailType.LOGIN_ALERT)
            .to(email)
            .userId(userId)
            .variables(variables)
            .locale(locale)
            .build();
        
        publishEmailNotification(event);
        log.debug("Published login alert email for user: {} with locale: {}", userId, locale);
    }
}