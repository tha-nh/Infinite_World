package com.infinite.notification.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "email_delivery_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailDeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "source_service", length = 100)
    private String sourceService;

    @Column(name = "source_action", length = 100)
    private String sourceAction;

    @Column(name = "to_email", nullable = false, length = 254)
    private String toEmail;

    @Column(name = "email_type", length = 50)
    private String emailType;

    @Column(name = "template_code", length = 100)
    private String templateCode;

    @Column(name = "subject")
    private String subject;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        requestedAt = Instant.now();
        if (status == null) {
            status = "PENDING";
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }
}
