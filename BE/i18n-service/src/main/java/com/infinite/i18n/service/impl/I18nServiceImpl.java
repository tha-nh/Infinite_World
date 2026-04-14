package com.infinite.i18n.service.impl;

import com.infinite.common.constant.StatusCode;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.i18n.model.I18nMessage;
import com.infinite.i18n.service.I18nService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
            log.info("Initializing i18n service...");
            
            // Create tables for each language if they don't exist
            List<String> languageList = Arrays.asList(languages.split(","));
            for (String lang : languageList) {
                createTableIfNotExists(lang.trim());
            }
            
            log.info("I18n service initialized successfully");
        } catch (Exception e) {
            log.error("Error initializing i18n service", e);
        }
    }

    /**
     * Create table for language if not exists
     * Table structure: id, key, message, key_1, key_2, ..., key_10, is_deleted, created_at, updated_at
     */
    @Override
    public void createTableIfNotExists(String language) {
        String tableName = "i18n_" + language;
        
        try {
            // Check if table exists using PostgreSQL catalog
            String checkTableSql = "SELECT EXISTS (SELECT 1 FROM information_schema.tables " +
                    "WHERE table_schema = 'public' AND table_name = ?)";
            
            Boolean tableExists = jdbcTemplate.queryForObject(checkTableSql, 
                    new Object[]{tableName}, Boolean.class);
            
            if (tableExists == null || !tableExists) {
                StringBuilder createTableSql = new StringBuilder();
                createTableSql.append(String.format(
                    "CREATE TABLE %s (\n", tableName
                ))
                .append("  id SERIAL PRIMARY KEY,\n")
                .append("  key VARCHAR(255) NOT NULL UNIQUE,\n")
                .append("  message TEXT NOT NULL,\n");
                
                // Add dynamic key columns (key_1 to key_10)
                for (int i = 1; i <= MAX_KEY_LEVELS; i++) {
                    createTableSql.append(String.format("  key_%d VARCHAR(255),\n", i));
                }
                
                createTableSql.append("  is_deleted BOOLEAN DEFAULT FALSE,\n")
                    .append("  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n")
                    .append("  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n")
                    .append(")");
                
                jdbcTemplate.execute(createTableSql.toString());
                
                // Create indexes
                String createIndexSql = String.format(
                    "CREATE INDEX idx_%s_key ON %s(key)",
                    tableName, tableName
                );
                jdbcTemplate.execute(createIndexSql);
                
                log.info("Created table {} for language: {}", tableName, language);
            }
        } catch (Exception e) {
            log.error("Error creating table for language: {}", language, e);
        }
    }

    /**
     * Validate key levels (between MIN_KEY_LEVELS and MAX_KEY_LEVELS)
     */
    private void validateKeyLevels(String key) {
        String[] keyParts = parseMessageKey(key);
        if (keyParts.length < MIN_KEY_LEVELS || keyParts.length > MAX_KEY_LEVELS) {
            throw new IllegalArgumentException(
                String.format("Key must have between %d and %d levels, got %d", 
                    MIN_KEY_LEVELS, MAX_KEY_LEVELS, keyParts.length)
            );
        }
    }

    /**
     * Parse key to extract key levels
     * Example: "user.profile.name" → ["user", "profile", "name"]
     */
    private String[] parseMessageKey(String key) {
        if (key == null || key.isEmpty()) {
            return new String[]{};
        }
        return key.split("\\.");
    }

    /**
     * Save message to database and cache
     * Auto-creates table if language doesn't exist
     */
    @Override
    public ApiResponse<Object> saveMessage(String language, I18nMessage msg) {
        try {
            // Validate key
            validateKeyLevels(msg.getKey());
            String[] keyParts = parseMessageKey(msg.getKey());
            String tableName = "i18n_" + language;
            
            // Auto-create table if not exists
            createTableIfNotExists(language);
            
            // Build INSERT/UPDATE SQL
            StringBuilder sql = new StringBuilder();
            
            sql.append(String.format("INSERT INTO %s (key, message", tableName));
            
            // Add key columns
            for (int i = 0; i < keyParts.length; i++) {
                sql.append(", key_").append(i + 1);
            }
            
            sql.append(") VALUES (?, ?");
            
            // Add values placeholders for key columns
            for (int i = 0; i < keyParts.length; i++) {
                sql.append(", ?");
            }
            
            sql.append(") ON CONFLICT (key) DO UPDATE SET message = EXCLUDED.message, updated_at = CURRENT_TIMESTAMP");
            
            // Add key updates
            for (int i = 0; i < keyParts.length; i++) {
                sql.append(", key_").append(i + 1).append(" = EXCLUDED.key_").append(i + 1);
            }
            
            // Prepare values
            Object[] values = new Object[2 + keyParts.length];
            values[0] = msg.getKey();
            values[1] = msg.getMessage();
            
            for (int i = 0; i < keyParts.length; i++) {
                values[2 + i] = keyParts[i];
            }
            
            jdbcTemplate.update(sql.toString(), values);
            
            // Cache to Redis
            cacheMessage(language, msg.getKey(), msg.getMessage());
            
            log.info("Message saved: {} in language: {}", msg.getKey(), language);
            
            return ApiResponse.builder()
                    .code(code(StatusCode.SUCCESS))
                    .message(message("i18n.message.created"))
                    .result(msg.getKey())
                    .build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid message key: {}", e.getMessage());
            return ApiResponse.builder()
                    .code(code(StatusCode.BAD_REQUEST))
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Error saving message for language: {}", language, e);
            return ApiResponse.builder()
                    .code(code(StatusCode.BAD_REQUEST))
                    .message("Error saving message: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Get message from Redis (with DB fallback)
     */
    @Override
    public String getMessage(String key, String language) {
        String redisKey = cachePrefix + language + ":" + key;
        
        try {
            // Try Redis first
            String cachedMessage = redisTemplate.opsForValue().get(redisKey);
            if (cachedMessage != null) {
                return cachedMessage;
            }
            
            // Fall back to database
            String tableName = "i18n_" + language;
            String sql = String.format(
                "SELECT message FROM %s WHERE key = ? AND is_deleted = FALSE",
                tableName
            );
            
            String dbMessage = jdbcTemplate.queryForObject(sql, new Object[]{key}, String.class);
            
            if (dbMessage != null) {
                // Cache it
                cacheMessage(language, key, dbMessage);
                return dbMessage;
            }
        } catch (Exception e) {
            log.warn("Message not found: key={}, language={}", key, language);
        }
        
        return key; // Return key if not found
    }

    /**
     * Cache message to Redis
     */
    private void cacheMessage(String language, String key, String messageValue) {
        try {
            String redisKey = cachePrefix + language + ":" + key;
            redisTemplate.opsForValue().set(redisKey, messageValue, cacheExpireHours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Error caching message: key={}, language={}", key, language, e);
        }
    }

    /**
     * Delete message (soft delete)
     */
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
                // Remove from Redis
                String redisKey = cachePrefix + language + ":" + key;
                redisTemplate.delete(redisKey);
                
                log.info("Message deleted: {} in language: {}", key, language);
                
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
            log.error("Error deleting message for language: {}", language, e);
            return ApiResponse.builder()
                    .code(code(StatusCode.BAD_REQUEST))
                    .message("Error deleting message: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Reload all messages from database to Redis
     */
    @Override
    public ApiResponse<Object> refreshCache() {
        try {
            log.info("Refreshing i18n cache...");
            
            // Clear all i18n keys from Redis
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
                    log.warn("Table {} might not exist yet", tableName);
                }
            }
            
            log.info("I18n cache refreshed successfully");
            
            return ApiResponse.builder()
                    .code(code(StatusCode.SUCCESS))
                    .message(message("i18n.cache.refreshed"))
                    .result("i18n cache refreshed from database")
                    .build();
        } catch (Exception e) {
            log.error("Error refreshing i18n cache", e);
            return ApiResponse.builder()
                    .code(code(StatusCode.BAD_REQUEST))
                    .message("Error refreshing cache: " + e.getMessage())
                    .build();
        }
    }
}
