package com.infinite.notification.service;

public interface WebSocketNotificationService {
    
    void sendToUser(String userId, String type, String title, String message);
    
    void broadcast(String type, String title, String message);
}