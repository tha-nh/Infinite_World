package com.infinite.notification.application.query;

import com.infinite.notification.dto.response.UserNotificationDto;
import com.infinite.notification.infrastructure.persistence.entity.UserNotification;
import com.infinite.notification.infrastructure.persistence.repository.UserNotificationRepository;
import com.infinite.notification.infrastructure.redis.UnreadCountCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetUserInboxQuery {

    private final UserNotificationRepository userNotificationRepository;
    private final UnreadCountCacheService unreadCountCacheService;

    @Transactional(readOnly = true)
    public Page<UserNotificationDto> execute(Long userId, Pageable pageable) {
        log.debug("Fetching inbox for user: userId={}", userId);
        
        Page<UserNotification> notifications = userNotificationRepository
            .findByUserIdAndNotDeleted(userId, pageable);
        
        return notifications.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        log.debug("Fetching unread count for user: userId={}", userId);
        return unreadCountCacheService.get(userId)
            .orElseGet(() -> {
                long count = userNotificationRepository.countUnreadByUserId(userId);
                unreadCountCacheService.set(userId, count);
                return count;
            });
    }

    private UserNotificationDto toDto(UserNotification entity) {
        return UserNotificationDto.builder()
            .id(entity.getId())
            .title(entity.getTitle())
            .body(entity.getBody())
            .type(entity.getType())
            .priority(entity.getPriority())
            .imageUrl(entity.getImageUrl())
            .actionPayload(entity.getActionPayload())
            .rewardPayload(entity.getRewardPayload())
            .status(entity.getStatus())
            .isClaimed(entity.getIsClaimed())
            .readAt(entity.getReadAt())
            .claimedAt(entity.getClaimedAt())
            .expireAt(entity.getExpireAt())
            .createdAt(entity.getCreatedAt())
            .build();
    }
}
