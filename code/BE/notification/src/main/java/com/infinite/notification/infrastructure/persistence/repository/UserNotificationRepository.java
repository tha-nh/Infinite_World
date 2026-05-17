package com.infinite.notification.infrastructure.persistence.repository;

import com.infinite.notification.infrastructure.persistence.entity.UserNotification;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    
    @Query("SELECT un FROM UserNotification un WHERE un.userId = :userId AND un.isDeleted = false ORDER BY un.createdAt DESC")
    Page<UserNotification> findByUserIdAndNotDeleted(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT COUNT(un) FROM UserNotification un WHERE un.userId = :userId AND un.status = 'UNREAD' AND un.isDeleted = false")
    long countUnreadByUserId(@Param("userId") Long userId);
    
    @Query("SELECT un FROM UserNotification un WHERE un.userId = :userId AND un.id = :id AND un.isDeleted = false")
    Optional<UserNotification> findByUserIdAndId(@Param("userId") Long userId, @Param("id") Long id);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT un FROM UserNotification un WHERE un.userId = :userId AND un.id = :id AND un.isDeleted = false")
    Optional<UserNotification> findByUserIdAndIdWithLock(@Param("userId") Long userId, @Param("id") Long id);
    
    @Modifying
    @Query("UPDATE UserNotification un SET un.status = 'READ', un.readAt = CURRENT_TIMESTAMP WHERE un.userId = :userId AND un.id = :id AND un.status = 'UNREAD'")
    int markAsRead(@Param("userId") Long userId, @Param("id") Long id);
    
    @Modifying
    @Query("UPDATE UserNotification un SET un.status = 'READ', un.readAt = CURRENT_TIMESTAMP WHERE un.userId = :userId AND un.status = 'UNREAD' AND un.isDeleted = false")
    int markAllAsRead(@Param("userId") Long userId);
    
    @Modifying
    @Query("UPDATE UserNotification un SET un.isDeleted = true WHERE un.userId = :userId AND un.id = :id")
    int softDelete(@Param("userId") Long userId, @Param("id") Long id);
}
