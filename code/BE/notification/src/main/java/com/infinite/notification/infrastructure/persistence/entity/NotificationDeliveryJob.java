package com.infinite.notification.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notification_delivery_job")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDeliveryJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Column(name = "job_type", nullable = false, length = 30)
    private String jobType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "total_target", nullable = false)
    private Long totalTarget;

    @Column(name = "processed_target", nullable = false)
    private Long processedTarget;

    @Column(name = "success_target", nullable = false)
    private Long successTarget;

    @Column(name = "failed_target", nullable = false)
    private Long failedTarget;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) {
            status = "PENDING";
        }
        if (totalTarget == null) {
            totalTarget = 0L;
        }
        if (processedTarget == null) {
            processedTarget = 0L;
        }
        if (successTarget == null) {
            successTarget = 0L;
        }
        if (failedTarget == null) {
            failedTarget = 0L;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
