package com.infinite.notification.service.impl;

import com.infinite.notification.service.WebSocketNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class WebSocketNotificationServiceImpl implements WebSocketNotificationService {
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public void sendToUser(String userId, String type, String title, String message) {
        if (redisTemplate == null) {
            log.warn("RedisTemplate not available, WebSocket notification not sent to user: {}", userId);
            return;
        }
        
        Map<String, Object> notification = Map.of(
            "userId", userId,
            "type", type,
            "title", title,
            "message", message,
            "timestamp", System.currentTimeMillis()
        );
        
        redisTemplate.convertAndSend("websocket:user:" + userId, notification);
        log.debug("Sent WebSocket notification to user: {}", userId);
    }
    
    @Override
    public void broadcast(String type, String title, String message) {
        if (redisTemplate == null) {
            log.warn("RedisTemplate not available, WebSocket broadcast not sent");
            return;
        }
        
        Map<String, Object> notification = Map.of(
            "type", type,
            "title", title,
            "message", message,
            "timestamp", System.currentTimeMillis(),
            "broadcast", true
        );
        
        redisTemplate.convertAndSend("websocket:broadcast", notification);
        log.debug("Broadcasted WebSocket notification");
    }
}