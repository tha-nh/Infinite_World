package com.infinite.notification.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "notification_template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "code", length = 100)
    private String code;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "channel_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> channelPayload;

    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "expire_at")
    private Instant expireAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

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
        if (priority == null) {
            priority = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
