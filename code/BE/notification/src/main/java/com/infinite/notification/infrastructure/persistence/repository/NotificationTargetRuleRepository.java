package com.infinite.notification.infrastructure.persistence.repository;

import com.infinite.notification.infrastructure.persistence.entity.NotificationTargetRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationTargetRuleRepository extends JpaRepository<NotificationTargetRule, Long> {
    
    List<NotificationTargetRule> findByNotificationId(Long notificationId);
}
