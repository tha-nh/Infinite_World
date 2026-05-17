package com.infinite.notification.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(
    name = "user_notification",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_notification", columnNames = {"user_id", "notification_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "priority", nullable = false)
    private Short priority;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "action_payload", columnDefinition = "jsonb")
    private Map<String, Object> actionPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reward_payload", columnDefinition = "jsonb")
    private Map<String, Object> rewardPayload;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    @Column(name = "is_claimed", nullable = false)
    private Boolean isClaimed;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "expire_at")
    private Instant expireAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (status == null) {
            status = "UNREAD";
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
        if (isClaimed == null) {
            isClaimed = false;
        }
        if (priority == null) {
            priority = 0;
        }
    }
}
