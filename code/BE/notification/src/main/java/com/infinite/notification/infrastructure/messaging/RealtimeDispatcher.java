package com.infinite.notification.infrastructure.messaging;

import com.infinite.notification.dto.event.WebSocketNotificationEvent;
import com.infinite.notification.infrastructure.persistence.entity.NotificationTemplate;
import com.infinite.notification.messaging.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeDispatcher {

    private final MessagePublisher messagePublisher;

    @Value("${messaging.topics.websocket:notification.websocket}")
    private String websocketTopic;

    public void dispatch(Long userId, NotificationTemplate template, long unreadCount) {
        try {
            WebSocketNotificationEvent event = WebSocketNotificationEvent.builder()
                    .eventId("ws-" + template.getId() + "-" + userId + "-" + UUID.randomUUID())
                    .timestamp(Instant.now())
                    .userId(String.valueOf(userId))
                    .type(template.getType())
                    .title(template.getTitle())
                    .message(template.getBody())
                    .broadcast(false)
                    .metadata(Map.of(
                            "notificationId", template.getId(),
                            "unreadCount", unreadCount
                    ))
                    .build();
            messagePublisher.publish(websocketTopic, String.valueOf(userId), event);
        } catch (Exception ex) {
            log.warn("Realtime dispatch failed: userId={}, notificationId={}", userId, template.getId(), ex);
        }
    }
}
