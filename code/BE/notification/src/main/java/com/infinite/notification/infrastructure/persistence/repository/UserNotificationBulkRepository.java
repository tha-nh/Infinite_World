package com.infinite.notification.infrastructure.persistence.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infinite.notification.infrastructure.persistence.entity.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class UserNotificationBulkRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.jpa.properties.hibernate.default_schema:INF_NOTI}")
    private String schema;

    public int insertIgnoreDuplicates(NotificationTemplate template, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }

        String sql = """
                INSERT INTO %s.user_notification
                (user_id, notification_id, title, body, type, priority, image_url, action_payload, reward_payload,
                 status, is_deleted, is_claimed, delivered_at, expire_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, 'UNREAD', false, false, ?, ?, ?)
                ON CONFLICT (user_id, notification_id) DO NOTHING
                """.formatted(schema);

        Instant now = Instant.now();
        int[][] results = jdbcTemplate.batchUpdate(sql, userIds, userIds.size(), (PreparedStatement ps, Long userId) -> {
            ps.setLong(1, userId);
            ps.setLong(2, template.getId());
            ps.setString(3, template.getTitle());
            ps.setString(4, template.getBody());
            ps.setString(5, template.getType());
            ps.setShort(6, template.getPriority() == null ? 0 : template.getPriority());
            ps.setString(7, template.getImageUrl());
            ps.setString(8, toJson(template.getActionPayload()));
            ps.setString(9, toJson(template.getRewardPayload()));
            ps.setTimestamp(10, Timestamp.from(now));
            ps.setTimestamp(11, template.getExpireAt() == null ? null : Timestamp.from(template.getExpireAt()));
            ps.setTimestamp(12, Timestamp.from(now));
        });

        int inserted = 0;
        for (int[] batchResults : results) {
            for (int result : batchResults) {
                if (result > 0 || result == PreparedStatement.SUCCESS_NO_INFO) {
                    inserted++;
                }
            }
        }
        return inserted;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return payload == null ? null : objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid notification payload", ex);
        }
    }
}
