package com.infinite.notification.application.command;

import com.infinite.common.exception.BadRequestException;
import com.infinite.common.exception.NotFoundException;
import com.infinite.common.util.I18n;
import com.infinite.notification.infrastructure.persistence.entity.UserNotification;
import com.infinite.notification.infrastructure.persistence.entity.UserNotificationClaimLog;
import com.infinite.notification.infrastructure.persistence.repository.UserNotificationClaimLogRepository;
import com.infinite.notification.infrastructure.persistence.repository.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimNotificationCommand {

    private final UserNotificationRepository userNotificationRepository;
    private final UserNotificationClaimLogRepository claimLogRepository;

    @Transactional
    public Map<String, Object> execute(Long userId, Long notificationId) {
        log.info("Claiming reward: userId={}, notificationId={}", userId, notificationId);
        
        // Find and lock notification with pessimistic write lock
        UserNotification notification = userNotificationRepository
            .findByUserIdAndIdWithLock(userId, notificationId)
            .orElseThrow(() -> new NotFoundException(I18n.msg("notification.not.found")));
        
        // Validate claim eligibility
        if (notification.getIsClaimed()) {
            throw new BadRequestException(I18n.msg("notification.claim.already"));
        }
        
        if (notification.getRewardPayload() == null || notification.getRewardPayload().isEmpty()) {
            throw new BadRequestException(I18n.msg("notification.claim.no.reward"));
        }
        
        if (notification.getExpireAt() != null && notification.getExpireAt().isBefore(Instant.now())) {
            throw new BadRequestException(I18n.msg("notification.claim.expired"));
        }
        
        // Mark as claimed
        notification.setIsClaimed(true);
        notification.setClaimedAt(Instant.now());
        userNotificationRepository.save(notification);
        
        // Create claim log
        String referenceCode = UUID.randomUUID().toString();
        UserNotificationClaimLog claimLog = UserNotificationClaimLog.builder()
            .userNotificationId(notification.getId())
            .userId(userId)
            .rewardPayload(notification.getRewardPayload())
            .claimedResult("SUCCESS")
            .referenceCode(referenceCode)
            .build();
        claimLogRepository.save(claimLog);
        
        log.info("Successfully claimed reward: userId={}, notificationId={}, referenceCode={}", 
            userId, notificationId, referenceCode);
        
        // TODO: Integrate with reward service to actually grant rewards
        
        return Map.of(
            "claimed", true,
            "referenceCode", referenceCode,
            "reward", notification.getRewardPayload()
        );
    }
}
