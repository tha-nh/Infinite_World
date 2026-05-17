package com.infinite.notification.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "user_notification_claim_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotificationClaimLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_notification_id", nullable = false)
    private Long userNotificationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reward_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rewardPayload;

    @Column(name = "claimed_result", nullable = false, length = 20)
    private String claimedResult;

    @Column(name = "reference_code", length = 100)
    private String referenceCode;

    @Column(name = "claimed_at", nullable = false, updatable = false)
    private Instant claimedAt;

    @PrePersist
    protected void onCreate() {
        claimedAt = Instant.now();
    }
}
