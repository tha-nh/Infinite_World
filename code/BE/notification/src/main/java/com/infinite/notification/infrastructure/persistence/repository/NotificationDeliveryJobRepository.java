package com.infinite.notification.infrastructure.persistence.repository;

import com.infinite.notification.infrastructure.persistence.entity.NotificationDeliveryJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationDeliveryJobRepository extends JpaRepository<NotificationDeliveryJob, Long> {
    
    List<NotificationDeliveryJob> findByNotificationId(Long notificationId);
    
    List<NotificationDeliveryJob> findByStatus(String status);
}
