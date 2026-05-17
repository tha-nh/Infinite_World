package com.infinite.notification.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class NotificationCleanupRepository {

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.jpa.properties.hibernate.default_schema:INF_NOTI}")
    private String schema;

    public int deleteUserNotificationsBefore(Instant threshold) {
        return jdbcTemplate.update(
                "DELETE FROM %s.user_notification WHERE created_at < ?".formatted(schema),
                Timestamp.from(threshold)
        );
    }

    public int deleteEmailDeliveryLogsBefore(Instant threshold) {
        return jdbcTemplate.update(
                "DELETE FROM %s.email_delivery_log WHERE created_at < ?".formatted(schema),
                Timestamp.from(threshold)
        );
    }

    public int deleteDeliveryBatchesBefore(Instant threshold) {
        return jdbcTemplate.update(
                "DELETE FROM %s.notification_delivery_batch WHERE created_at < ? AND status IN ('COMPLETED', 'FAILED', 'CANCELED')".formatted(schema),
                Timestamp.from(threshold)
        );
    }

    public int deleteDeliveryJobsBefore(Instant threshold) {
        return jdbcTemplate.update(
                "DELETE FROM %s.notification_delivery_job WHERE created_at < ? AND status IN ('COMPLETED', 'FAILED', 'CANCELED')".formatted(schema),
                Timestamp.from(threshold)
        );
    }
}
