package com.infinite.notification.infrastructure.persistence.repository;

import com.infinite.notification.infrastructure.persistence.entity.EmailDeliveryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailDeliveryLogRepository extends JpaRepository<EmailDeliveryLog, Long> {
    
    Optional<EmailDeliveryLog> findByEventId(String eventId);
    
    List<EmailDeliveryLog> findByStatus(String status);
    
    List<EmailDeliveryLog> findByNotificationId(Long notificationId);

    @Query("SELECT e FROM EmailDeliveryLog e WHERE (:status IS NULL OR :status = '' OR e.status = :status) ORDER BY e.createdAt DESC")
    Page<EmailDeliveryLog> findByStatusOptional(@Param("status") String status, Pageable pageable);
}
