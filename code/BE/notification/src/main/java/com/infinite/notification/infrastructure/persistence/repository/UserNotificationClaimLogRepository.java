package com.infinite.notification.infrastructure.persistence.repository;

import com.infinite.notification.infrastructure.persistence.entity.UserNotificationClaimLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserNotificationClaimLogRepository extends JpaRepository<UserNotificationClaimLog, Long> {
    
    List<UserNotificationClaimLog> findByUserNotificationId(Long userNotificationId);
    
    List<UserNotificationClaimLog> findByUserId(Long userId);
}
