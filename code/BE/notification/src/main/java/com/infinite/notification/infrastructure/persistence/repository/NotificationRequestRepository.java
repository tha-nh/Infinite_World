package com.infinite.notification.infrastructure.persistence.repository;

import com.infinite.notification.infrastructure.persistence.entity.NotificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRequestRepository extends JpaRepository<NotificationRequest, Long> {
    
    Optional<NotificationRequest> findByEventId(String eventId);
    
    Optional<NotificationRequest> findByIdempotencyKey(String idempotencyKey);
    
    boolean existsByIdempotencyKey(String idempotencyKey);
}
