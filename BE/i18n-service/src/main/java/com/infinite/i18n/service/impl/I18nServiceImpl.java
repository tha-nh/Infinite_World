package com.infinite.i18n.service.impl;

import com.infinite.common.constant.StatusCode;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.i18n.model.I18nMessage;
import com.infinite.i18n.service.I18nService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.infinite.common.dto.response.Response.code;
import static com.infinite.common.dto.response.Response.message;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class I18nServiceImpl implements I18nService {

    final RedisTemplate<String, String> redisTemplate;
    final JdbcTemplate jdbcTemplate;

    @Value("${i18n.cache-prefix:i18n:}")
    private String cachePrefix;

    @Value("${i18n.cache-expire-hours:24}")
    private long cacheExpireHours;

    @Value("${i18n.languages:en,vi}")
    private String languages;

    private static final int MAX_KEY_LEVELS = 10;
    private static final int MIN_KEY_LEVELS = 1;

    @PostConstruct
    public void initI18nService() {
        try {
            List<String> languageList = Arrays.asList(languages.split(","));
            for (String lang : languageList) {
                createTableIfNotExists(lang.trim());
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void createTableIfNotExists(String language) {
        String tableName = "i18n_" + language;

        try {
            String checkTableSql = "SELECT EXISTS (SELECT 1 FROM information_schema.tables " +
                    "WHERE table_schema = 'public' AND table_name = ?)";

            Boolean tableExists = jdbcTemplate.queryForObject(checkTableSql,
                    new Object[]{tableName}, Boolean.class);

            if (tableExists == null || !tableExists) {
                StringBuilder createTableSql = new StringBuilder();
                createTableSql.append(String.format(
                                "CREATE TABLE %s (\\n", tableName
                        ))
                        .append("  id SERIAL PRIMARY KEY,\\n")
                        .append("  key VARCHAR(255) NOT NULL UNIQUE,\\n")
                        .append("  message TEXT NOT NULL,\\n");

                for (int i = 1; i <= MAX_KEY_LEVELS; i++) {
                    createTableSql.append(String.format("  key_%d VARCHAR(255),\\n", i));
                }

                createTableSql.append("  is_deleted BOOLEAN DEFAULT FALSE,\\n")
                        .append("  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\\n")
                        .append("  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\\n")
                        .append(")");

                jdbcTemplate.execute(createTableSql.toString());

                String createIndexSql = String.format(
                        "CREATE INDEX idx_%s_key ON %s(key)",
                        tableName, tableName
                );
                jdbcTemplate.execute(createIndexSql);
            }
        } catch (Exception e) {
        }
    }

    private void validateKeyLevels(String key) {
        String[] keyParts = parseMessageKey(key);
        if (keyParts.length < MIN_KEY_LEVELS || keyParts.length > MAX_KEY_LEVELS) {
            throw new IllegalArgumentException(
                    String.format("Key must have between %d and %d levels, got %d",
                            MIN_KEY_LEVELS, MAX_KEY_LEVELS, keyParts.length)
            );
        }
    }

    private String[] parseMessageKey(String key) {
        if (key == null || key.isEmpty()) {
            return new String[]{};
        }
        return key.split("\\\\.");
    }

    @Override
    public ApiResponse<Object> saveMessage(String language, I18nMessage msg) {
        try {
            validateKeyLevels(msg.getKey());
            String[] keyParts = parseMessageKey(msg.getKey());
            String tableName = "i18n_" + language;

            createTableIfNotExists(language);

            StringBuilder sql = new StringBuilder();

            sql.append(String.format("INSERT INTO %s (key, message", tableName));

            for (int i = 0; i < keyParts.length; i++) {
                sql.append(", key_").append(i + 1);
            }

            sql.append(") VALUES (?, ?");

            for (int i = 0; i < keyParts.length; i++) {
                sql.append(", ?");
            }

            sql.append(") ON CONFLICT (key) DO UPDATE SET message = EXCLUDED.message, updated_at = CURRENT_TIMESTAMP");

            for (int i = 0; i < keyParts.length; i++) {
                sql.append(", key_").append(i + 1).append(" = EXCLUDED.key_").append(i + 1);
            }

            Object[] values = new Object[2 + keyParts.length];
            values[0] = msg.getKey();
            values[1] = msg.getMessage();

            for (int i = 0; i < keyParts.length; i++) {
                values[2 + i] = keyParts[i];
            }

            jdbcTemplate.update(sql.toString(), values);

            cacheMessage(language, msg.getKey(), msg.getMessage());

            return ApiResponse.builder()
                    .code(code(StatusCode.SUCCESS))
                    .message(message("i18n.message.created"))
                    .result(msg.getKey())
                    .build();
        } catch (IllegalArgumentException e) {
            return ApiResponse.builder()
                    .code(code(StatusCode.BAD_REQUEST))
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            return ApiResponse.builder()
                    .code(code(StatusCode.BAD_REQUEST))
                    .message("Error saving message: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public String getMessage(String key, String language) {
        String redisKey = cachePrefix + language + ":" + key;

        try {
            String cachedMessage = redisTemplate.opsForValue().get(redisKey);
            if (cachedMessage != null) {
                return cachedMessage;
            }

            String tableName = "i18n_" + language;
            String sql = String.format(
                    "SELECT message FROM %s WHERE key = ? AND is_deleted = FALSE",
                    tableName
            );

            String dbMessage = jdbcTemplate.queryForObject(sql, new Object[]{key}, String.class);

            if (dbMessage != null) {
                cacheMessage(language, key, dbMessage);
                return dbMessage;
            }
        } catch (Exception e) {
        }

        return key;
    }

    private void cacheMessage(String language, String key, String messageValue) {
        try {
            String redisKey = cachePrefix + language + ":" + key;
            redisTemplate.opsForValue().set(redisKey, messageValue, cacheExpireHours, TimeUnit.HOURS);
        } catch (Exception e) {
        }
    }

    @Override
    public ApiResponse<Object> deleteMessage(String language, String key) {
        try {
            String tableName = "i18n_" + language;
            String sql = String.format(
                    "UPDATE %s SET is_deleted = TRUE WHERE key = ?",
                    tableName
            );

            int updated = jdbcTemplate.update(sql, key);

            if (updated > 0) {
                String redisKey = cachePrefix + language + ":" + key;
                redisTemplate.delete(redisKey);

                return ApiResponse.builder()
                        .code(code(StatusCode.SUCCESS))
                        .message(message("i18n.message.deleted"))
                        .result(key)
                        .build();
            }

            return ApiResponse.builder()
                    .code(code(StatusCode.BAD_REQUEST))
                    .message("Message not found: " + key)
                    .build();
        } catch (Exception e) {
            return ApiResponse.builder()
                    .code(code(StatusCode.BAD_REQUEST))
                    .message("Error deleting message: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ApiResponse<Object> refreshCache() {
        try {
            redisTemplate.delete(redisTemplate.keys(cachePrefix + "*"));

            List<String> languageList = Arrays.asList(languages.split(","));
            for (String lang : languageList) {
                String tableName = "i18n_" + lang.trim();
                String sql = String.format(
                        "SELECT key, message FROM %s WHERE is_deleted = FALSE",
                        tableName
                );

                try {
                    jdbcTemplate.query(sql, rs -> {
                        String key = rs.getString("key");
                        String msgValue = rs.getString("message");
                        cacheMessage(lang.trim(), key, msgValue);
                    });
                } catch (Exception e) {
                }
            }

            return ApiResponse.builder()
                    .code(code(StatusCode.SUCCESS))
                    .message(message("i18n.cache.refreshed"))
                    .build();
        } catch (Exception e) {
            return ApiResponse.builder()
                    .code(code(StatusCode.BAD_REQUEST))
                    .message("Error refreshing cache: " + e.getMessage())
                    .build();
        }
    }
}
