package com.infinite.user.service;

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
    
    // ========== Helper methods ==========
    
    /**
     * Helper: Send OTP email
     */
    public void sendOtpEmail(String email, String userId, String otp, String type) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        
        EmailNotificationEvent event = EmailNotificationEvent.builder()
            .to(email)
            .subject("OTP Verification")
            .userId(userId)
            .metadata(Map.of("type", type, "otp", otp))
            .locale(locale)
            .build();
        
        publishEmailNotification(event);
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
    // ========== New methods for user status changes and account verification ==========
    
    /**
     * Helper: Send account verification email
     */
    public void sendAccountVerificationEmail(String email, String userId, String username, String verificationToken) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        
        AccountVerificationEvent event = AccountVerificationEvent.builder()
            .to(email)
            .userId(userId)
            .username(username)
            .verificationToken(verificationToken)
            .locale(locale)
            .build();
        
        try {
            kafkaTemplate.send("account.verification", userId, event);
            log.debug("Published account verification to topic: account.verification for user: {} with locale: {}", userId, locale);
        } catch (Exception e) {
            log.error("Failed to publish account verification event", e);
            throw new RuntimeException("Failed to publish account verification", e);
        }
    }
    
    /**
     * Helper: Send user status change notification
     */
    public void sendUserStatusChangeNotification(String email, String userId, String username, 
                                                String action, LocalDateTime lockTime, String performedBy) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        
        UserStatusChangeEvent event = UserStatusChangeEvent.builder()
            .to(email)
            .userId(userId)
            .username(username)
            .action(action)
            .lockTime(lockTime)
            .performedBy(performedBy)
            .locale(locale)
            .build();
        
        try {
            kafkaTemplate.send("user.status.change", userId, event);
            log.debug("Published user status change to topic: user.status.change for user: {} action: {} with locale: {}", userId, action, locale);
        } catch (Exception e) {
            log.error("Failed to publish user status change event", e);
            throw new RuntimeException("Failed to publish user status change", e);
        }
    }
}