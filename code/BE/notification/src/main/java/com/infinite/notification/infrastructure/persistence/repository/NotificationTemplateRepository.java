package com.infinite.notification.infrastructure.persistence.repository;

import com.infinite.notification.infrastructure.persistence.entity.NotificationTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    
    Optional<NotificationTemplate> findByCode(String code);
    
    List<NotificationTemplate> findByRequestId(Long requestId);
    
    List<NotificationTemplate> findByStatus(String status);
    
    @Query("SELECT nt FROM NotificationTemplate nt WHERE nt.status = :status ORDER BY nt.createdAt DESC")
    Page<NotificationTemplate> findByStatusPaginated(@Param("status") String status, Pageable pageable);
}
