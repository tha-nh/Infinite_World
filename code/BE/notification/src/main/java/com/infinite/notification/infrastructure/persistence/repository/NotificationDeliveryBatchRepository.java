package com.infinite.notification.infrastructure.persistence.repository;

import com.infinite.notification.infrastructure.persistence.entity.NotificationDeliveryBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationDeliveryBatchRepository extends JpaRepository<NotificationDeliveryBatch, Long> {
    
    List<NotificationDeliveryBatch> findByJobId(Long jobId);
    
    List<NotificationDeliveryBatch> findByStatus(String status);
    
    List<NotificationDeliveryBatch> findByJobIdAndStatus(Long jobId, String status);
}
