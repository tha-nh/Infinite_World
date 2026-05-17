package com.infinite.notification.application.command;

import com.infinite.common.exception.NotFoundException;
import com.infinite.common.util.I18n;
import com.infinite.notification.infrastructure.persistence.entity.UserNotification;
import com.infinite.notification.infrastructure.persistence.repository.UserNotificationRepository;
import com.infinite.notification.infrastructure.redis.UnreadCountCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarkNotificationAsReadCommand {

    private final UserNotificationRepository userNotificationRepository;
    private final UnreadCountCacheService unreadCountCacheService;

    @Transactional
    public void execute(Long userId, Long notificationId) {
        log.info("Marking notification as read: userId={}, notificationId={}", userId, notificationId);
        
        int updated = userNotificationRepository.markAsRead(userId, notificationId);
        
        if (updated == 0) {
            throw new NotFoundException(I18n.msg("notification.not.found"));
        }
        unreadCountCacheService.increment(userId, -1);
        
        log.info("Successfully marked notification as read: userId={}, notificationId={}", userId, notificationId);
    }

    @Transactional
    public int executeAll(Long userId) {
        log.info("Marking all notifications as read: userId={}", userId);
        
        int updated = userNotificationRepository.markAllAsRead(userId);
        unreadCountCacheService.set(userId, 0);
        
        log.info("Successfully marked {} notifications as read: userId={}", updated, userId);
        return updated;
    }

    @Transactional
    public void delete(Long userId, Long notificationId) {
        log.info("Deleting notification: userId={}, notificationId={}", userId, notificationId);
        
        UserNotification notification = userNotificationRepository.findByUserIdAndId(userId, notificationId)
                .orElseThrow(() -> new NotFoundException(I18n.msg("notification.not.found")));
        boolean unread = "UNREAD".equals(notification.getStatus());

        int updated = userNotificationRepository.softDelete(userId, notificationId);
        
        if (updated == 0) {
            throw new NotFoundException(I18n.msg("notification.not.found"));
        }
        if (unread) {
            unreadCountCacheService.increment(userId, -1);
        }
        
        log.info("Successfully deleted notification: userId={}, notificationId={}", userId, notificationId);
    }
}
