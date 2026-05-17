package com.infinite.notification.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notification_delivery_batch")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDeliveryBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "batch_no", nullable = false)
    private Integer batchNo;

    @Column(name = "cursor_value")
    private Long cursorValue;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "expected_count", nullable = false)
    private Integer expectedCount;

    @Column(name = "processed_count", nullable = false)
    private Integer processedCount;

    @Column(name = "success_count", nullable = false)
    private Integer successCount;

    @Column(name = "failed_count", nullable = false)
    private Integer failedCount;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

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
        if (expectedCount == null) {
            expectedCount = 0;
        }
        if (processedCount == null) {
            processedCount = 0;
        }
        if (successCount == null) {
            successCount = 0;
        }
        if (failedCount == null) {
            failedCount = 0;
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
